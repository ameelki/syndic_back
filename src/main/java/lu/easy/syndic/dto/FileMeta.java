package lu.easy.syndic.dto;

/** Description d'un fichier joint (sans son contenu). */
public record FileMeta(Long id, String fileName, String contentType) {}
