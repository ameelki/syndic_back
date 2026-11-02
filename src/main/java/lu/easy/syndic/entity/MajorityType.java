package lu.easy.syndic.entity;

/** Règle de majorité d'une résolution. */
public enum MajorityType {
    SIMPLE,      // plus de « pour » que de « contre » (abstentions non comptées)
    ABSOLUTE,    // plus de la moitié de tous les tantièmes
    TWO_THIRDS,  // au moins 2/3 des voix exprimées (pour + contre)
    UNANIMITY    // tous les tantièmes votent « pour »
}
