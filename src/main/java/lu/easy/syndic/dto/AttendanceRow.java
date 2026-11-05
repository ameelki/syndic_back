package lu.easy.syndic.dto;

/** Ligne de la feuille de présence : un copropriétaire, ses lots, ses tantièmes et sa présence. */
public record AttendanceRow(Long ownerId, String name, String lots, int ownWeight, boolean present,
                            String proxyTo, int proxiesReceived) {}
