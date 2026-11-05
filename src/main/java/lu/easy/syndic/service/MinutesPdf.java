package lu.easy.syndic.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lu.easy.syndic.dto.AttendanceRow;
import lu.easy.syndic.dto.Dtos.AssemblyView;
import lu.easy.syndic.dto.Dtos.ResolutionView;
import lu.easy.syndic.dto.Dtos.VoterView;
import lu.easy.syndic.entity.Assembly;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Génère le procès-verbal d'une assemblée au format PDF (présences, quorum, résultats par résolution). */
public final class MinutesPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final Color GREY = new Color(98, 112, 120);

    private MinutesPdf() {}

    public static byte[] build(Assembly asm, AssemblyView view, List<AttendanceRow> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10.5f);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, GREY);

            doc.add(new Paragraph("Procès-verbal d'assemblée", h1));
            doc.add(new Paragraph(asm.title, h2));
            Paragraph meta = new Paragraph(
                    (asm.meetingDate == null ? "" : "Date : " + asm.meetingDate.format(DATE) + "\n")
                            + (blank(asm.location) ? "" : "Lieu : " + asm.location + "\n")
                            + (blank(asm.chairman) ? "" : "Président de séance : " + asm.chairman + "\n")
                            + "Statut : " + status(asm), body);
            meta.setSpacingBefore(8);
            meta.setSpacingAfter(10);
            doc.add(meta);

            if (!blank(asm.agenda)) {
                doc.add(new Paragraph("Ordre du jour", h2));
                Paragraph ag = new Paragraph(asm.agenda, body);
                ag.setSpacingAfter(10);
                doc.add(ag);
            }

            doc.add(new Paragraph("Présences et quorum", h2));
            long present = rows.stream().filter(AttendanceRow::present).count();
            int presentWeight = rows.stream().filter(AttendanceRow::present).mapToInt(AttendanceRow::ownWeight).sum();
            Paragraph q = new Paragraph(present + " copropriétaire(s) présent(s) sur " + rows.size() + " (" + presentWeight + " tantièmes présents). "
                    + "Tantièmes totaux : " + view.totalWeight() + ". Tantièmes ayant voté : " + view.votedWeight() + ". "
                    + (asm.quorumPercent == null || asm.quorumPercent <= 0 ? "Aucun quorum exigé."
                    : "Quorum exigé : " + asm.quorumPercent + " % — " + (view.quorumReached() ? "atteint." : "non atteint.")), body);
            q.setSpacingBefore(4);
            q.setSpacingAfter(6);
            doc.add(q);

            PdfPTable att = new PdfPTable(new float[]{3.2f, 1.6f, 1.2f, 1.2f, 2.4f});
            att.setWidthPercentage(100);
            for (String h : new String[]{"Copropriétaire", "Lots", "Tantièmes", "Présent", "Procuration"}) head(att, h, bold);
            for (AttendanceRow r : rows) {
                cell(att, r.name(), body);
                cell(att, r.lots(), body);
                cell(att, String.valueOf(r.ownWeight()), body);
                cell(att, r.present() ? "Oui" : "Non", body);
                cell(att, r.proxyTo() != null ? "Donnée à " + r.proxyTo()
                        : r.proxiesReceived() > 0 ? "Reçoit " + r.proxiesReceived() : "", body);
            }
            att.setSpacingAfter(12);
            doc.add(att);

            doc.add(new Paragraph("Résolutions", h2));
            int i = 1;
            for (ResolutionView r : view.resolutions()) {
                Paragraph t = new Paragraph(i++ + ". " + r.title(), bold);
                t.setSpacingBefore(8);
                doc.add(t);
                if (!blank(r.description())) doc.add(new Paragraph(r.description(), body));
                doc.add(new Paragraph("Majorité requise : " + majority(String.valueOf(r.majority()))
                        + "   |   Pour : " + r.forWeight() + "   Contre : " + r.againstWeight() + "   Abstention : " + r.abstainWeight()
                        + " (tantièmes)   |   Résultat : " + result(r.result()), body));
                if (r.voters() != null && !r.voters().isEmpty()) {
                    StringBuilder sb = new StringBuilder("Détail : ");
                    for (VoterView v : r.voters()) sb.append(v.name()).append(" (").append(v.weight()).append(") ").append(choice(String.valueOf(v.choice()))).append(" ; ");
                    doc.add(new Paragraph(sb.toString(), small));
                }
            }
            if (view.resolutions().isEmpty()) doc.add(new Paragraph("Aucune résolution.", body));

            if (!blank(asm.minutesNote)) {
                Paragraph n = new Paragraph("Remarques", h2);
                n.setSpacingBefore(12);
                doc.add(n);
                doc.add(new Paragraph(asm.minutesNote, body));
            }

            Paragraph sign = new Paragraph("\n\nSignature du président de séance :\n\n\n______________________________", body);
            sign.setSpacingBefore(18);
            doc.add(sign);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le PDF", e);
        }
    }

    private static void head(PdfPTable t, String s, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(s, f));
        c.setBackgroundColor(new Color(231, 236, 237));
        c.setPadding(5);
        t.addCell(c);
    }

    private static void cell(PdfPTable t, String s, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(s == null ? "" : s, f));
        c.setPadding(5);
        t.addCell(c);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String status(Assembly a) {
        return switch (a.status) {
            case PLANNED -> "Planifiée";
            case OPEN -> "Vote ouvert";
            case CLOSED -> "Clôturée";
        };
    }

    private static String majority(String m) {
        return switch (m) {
            case "ABSOLUTE" -> "absolue (plus de la moitié des tantièmes)";
            case "TWO_THIRDS" -> "deux tiers des voix exprimées";
            case "UNANIMITY" -> "unanimité";
            default -> "simple";
        };
    }

    private static String result(String r) {
        return switch (r) {
            case "ADOPTED" -> "ADOPTÉE";
            case "REJECTED" -> "REJETÉE";
            case "NO_QUORUM" -> "QUORUM NON ATTEINT";
            default -> "EN COURS";
        };
    }

    private static String choice(String c) {
        return switch (c) {
            case "FOR" -> "pour";
            case "AGAINST" -> "contre";
            default -> "abstention";
        };
    }
}
