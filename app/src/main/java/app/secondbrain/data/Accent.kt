package app.secondbrain.data

/** ARGB values of one accent in one theme: the colour itself, its container, and text on that container. */
data class AccentTones(val color: Long, val container: Long, val onContainer: Long)

/** The one colour the otherwise black-and-white app uses. Declared in the order the picker shows them. */
enum class Accent(val label: String, val light: AccentTones, val dark: AccentTones) {
    CRIMSON("Crimson", AccentTones(0xFFC52C5C, 0xFFFED8DD, 0xFF3F0317), AccentTones(0xFFF67491, 0xFF702337, 0xFFFED8DD)),
    RED("Red", AccentTones(0xFFC4162B, 0xFFFFDADC, 0xFF5C0010), AccentTones(0xFFFF7A82, 0xFF6E0E1A, 0xFFFFDADC)),
    CORAL("Coral", AccentTones(0xFFBC3D17, 0xFFFEDAD0, 0xFF3F0800), AccentTones(0xFFFE8D6E, 0xFF712712, 0xFFFEDAD0)),
    ORANGE("Orange", AccentTones(0xFFB23C00, 0xFFFFDBC9, 0xFF431300), AccentTones(0xFFFF8F4F, 0xFF6E2800, 0xFFFFDBC9)),
    BROWN("Brown", AccentTones(0xFF8E6040, 0xFFFFDBC3, 0xFF341600), AccentTones(0xFFD2A78B, 0xFF603616, 0xFFFFDBC3)),
    AMBER("Amber", AccentTones(0xFF855400, 0xFFFFDF9E, 0xFF2A1800), AccentTones(0xFFFFC14D, 0xFF5C4000, 0xFFFFDF9E)),
    YELLOW("Yellow", AccentTones(0xFF7B6A08, 0xFFF0E4AB, 0xFF261F00), AccentTones(0xFFF1DC73, 0xFF4E4201, 0xFFF0E4AB)),
    OLIVE("Olive", AccentTones(0xFF6A6E2B, 0xFFE4E9AF, 0xFF202100), AccentTones(0xFFBEC38A, 0xFF444603, 0xFFE4E9AF)),
    LIME("Lime", AccentTones(0xFF4A6800, 0xFFD4F57A, 0xFF141F00), AccentTones(0xFFC2F044, 0xFF3A5200, 0xFFD4F57A)),
    GREEN("Green", AccentTones(0xFF0B7438, 0xFFBDF2CB, 0xFF00210B), AccentTones(0xFF4FDB84, 0xFF005226, 0xFFBDF2CB)),
    EMERALD("Emerald", AccentTones(0xFF057957, 0xFFB4F3D7, 0xFF002619), AccentTones(0xFF72D4AC, 0xFF044F38, 0xFFB4F3D7)),
    TEAL("Teal", AccentTones(0xFF00716F, 0xFFA6F1EE, 0xFF00201F), AccentTones(0xFF3FD9D4, 0xFF00504E, 0xFFA6F1EE)),
    CYAN("Cyan", AccentTones(0xFF047488, 0xFFB0EFFF, 0xFF00242C), AccentTones(0xFF5FD3ED, 0xFF034B58, 0xFFB0EFFF)),
    SKY("Sky", AccentTones(0xFF0571A2, 0xFFC9E8FD, 0xFF002236), AccentTones(0xFF62C2FD, 0xFF044869, 0xFFC9E8FD)),
    BLUE("Blue", AccentTones(0xFF0A4BFF, 0xFFDAE2FF, 0xFF001551), AccentTones(0xFF7C9DFF, 0xFF0A3299, 0xFFDAE2FF)),
    INDIGO("Indigo", AccentTones(0xFF535CD3, 0xFFDBE2FE, 0xFF151749), AccentTones(0xFF8B9BFE, 0xFF333A7C, 0xFFDBE2FE)),
    VIOLET("Violet", AccentTones(0xFF6B2FD9, 0xFFEADDFF, 0xFF23005C), AccentTones(0xFFC3A0FF, 0xFF4A1B9E, 0xFFEADDFF)),
    PURPLE("Purple", AccentTones(0xFF8F47C3, 0xFFECDAFE, 0xFF2B0E3F), AccentTones(0xFFCD93FD, 0xFF532F6F, 0xFFECDAFE)),
    MAGENTA("Magenta", AccentTones(0xFFB22FA6, 0xFFFFD3F7, 0xFF360832), AccentTones(0xFFED7FDF, 0xFF62285C, 0xFFFFD3F7)),
    PINK("Pink", AccentTones(0xFFBE0066, 0xFFFFD9E5, 0xFF3F001F), AccentTones(0xFFFF6FB0, 0xFF80004A, 0xFFFFD9E5)),
    ROSE("Rose", AccentTones(0xFFA04F68, 0xFFFDD8E1, 0xFF3E041C), AccentTones(0xFFEA9EB3, 0xFF6F233E, 0xFFFDD8E1)),
    SAGE("Sage", AccentTones(0xFF547054, 0xFFCDEDCD, 0xFF0C260E), AccentTones(0xFFACC6AC, 0xFF2F4A30, 0xFFCDEDCD)),
    STEEL("Steel", AccentTones(0xFF4E6D87, 0xFFCCE7FE, 0xFF032137), AccentTones(0xFF9FBBD4, 0xFF28455E, 0xFFCCE7FE)),
    GRAPHITE("Graphite", AccentTones(0xFF6A6A6A, 0xFFE3E3E3, 0xFF1F1F1F), AccentTones(0xFFABABAB, 0xFF424242, 0xFFE3E3E3)),
    ;

    companion object {
        val Default = GREEN
    }
}
