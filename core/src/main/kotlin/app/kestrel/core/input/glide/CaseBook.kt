package app.kestrel.core.input.glide

/**
 * Where a capital letter belongs inside a sentence (owner R18b: "auto capitalize words at the beginning of sentences and where it makes sense to auto capitalize"). The start of a sentence is the controller's job
 * ([app.kestrel.core.input.editor.InputController.startsSentence]); this is the other half: the words that are written with a capital wherever they stand (days, months, countries, languages, common first names,
 * brand names) and the words THIS person writes that way (a name they type twice with its capital is theirs from then on: [PersonalModel.addCase]).
 *
 * The built-in list is small on purpose: a word that is also an everyday word (will, mark, rose, grace, turkey, polish, windows, zoom, hunter ...) is left OUT, or only capitalised after the words that make it a date
 * (may, march), because a wrong capital in the middle of a sentence is worse than a missing one. One Backspace right after the capital is put in takes it back, and the keyboard learns the word is written small.
 */
object CaseBook {
    /** Days, months, holidays, languages, nationalities, religions, continents, countries, states, cities, first names, brands: each in the way it is written. */
    private const val DATA_PLACES = "Monday Tuesday Wednesday Thursday Friday Saturday Sunday January February April June July August September October November December " +
        "Christmas Easter Thanksgiving Halloween Hanukkah Ramadan Diwali Valentine's Passover Eid Kwanzaa " +
        "English Spanish French German Italian Portuguese Dutch Swedish Norwegian Danish Finnish Russian Ukrainian Greek Turkish Arabic Hebrew Persian Hindi Urdu Bengali Punjabi Tamil Telugu Chinese Mandarin Cantonese Japanese Korean Vietnamese Thai Indonesian Filipino Tagalog Swahili Latin " +
        "American British Canadian Mexican Brazilian Argentinian Colombian Peruvian Chilean Cuban Irish Scottish Welsh Australian European African Asian Indian Pakistani Egyptian Israeli Iranian Iraqi Saudi Nigerian Kenyan Ethiopian Jamaican " +
        "Christian Catholic Protestant Muslim Jewish Buddhist Hindu Sikh Mormon Islam Christianity Judaism Buddhism Hinduism Bible Quran Torah Jesus " +
        "Africa Asia Europe Antarctica Australia Oceania Canada Mexico Brazil Argentina Colombia Peru Cuba Jamaica Haiti Ecuador Bolivia Venezuela Uruguay Paraguay Guatemala Honduras Nicaragua Panama " +
        "Ireland Scotland England Wales Britain France Germany Italy Spain Portugal Netherlands Belgium Switzerland Austria Sweden Norway Denmark Finland Iceland Poland Ukraine Russia Greece Romania Hungary Czechia Croatia Serbia " +
        "China Japan Korea Vietnam Thailand Indonesia Philippines Malaysia Singapore India Pakistan Bangladesh Nepal Afghanistan Iran Iraq Israel Palestine Syria Lebanon Jordan Egypt Libya Morocco Algeria Tunisia Sudan Ethiopia Kenya Uganda Tanzania Nigeria Ghana Senegal Somalia Zimbabwe Zambia Botswana Namibia " +
        "Alabama Alaska Arizona Arkansas California Colorado Connecticut Delaware Florida Georgia Hawaii Idaho Illinois Indiana Iowa Kansas Kentucky Louisiana Maine Maryland Massachusetts Michigan Minnesota Mississippi Missouri Montana Nebraska Nevada Ohio Oklahoma Oregon Pennsylvania Tennessee Texas Utah Vermont Virginia Washington Wisconsin Wyoming Ontario Quebec Alberta Manitoba " +
        "London Paris Berlin Madrid Rome Lisbon Vienna Prague Warsaw Moscow Athens Istanbul Dublin Edinburgh Glasgow Manchester Liverpool Birmingham Amsterdam Brussels Zurich Geneva Stockholm Oslo Copenhagen Helsinki Budapest Bucharest Kyiv " +
        "Tokyo Osaka Kyoto Seoul Beijing Shanghai Taipei Bangkok Hanoi Manila Jakarta Delhi Mumbai Bangalore Kolkata Karachi Lahore Dhaka Tehran Baghdad Jerusalem Dubai Cairo Lagos Nairobi Johannesburg Casablanca " +
        "Chicago Houston Philadelphia Dallas Austin Denver Seattle Boston Atlanta Miami Orlando Portland Detroit Memphis Nashville Baltimore Milwaukee Sacramento Honolulu Anchorage Brooklyn Manhattan Hollywood Vegas Toronto Vancouver Montreal Ottawa Calgary Winnipeg " +
        "Guadalajara Monterrey Tijuana Cancun Acapulco Oaxaca Puebla Bogota Lima Santiago Caracas Quito Havana Sydney Melbourne Brisbane Perth Auckland Wellington"

    private const val DATA_NAMES = "James John Robert Michael William David Richard Joseph Thomas Charles Christopher Daniel Matthew Anthony Donald Steven Paul Andrew Joshua Kenneth Kevin Brian George Timothy Ronald Jason Edward Jeffrey Ryan Jacob Gary Nicholas Eric Jonathan Stephen Larry Justin Scott Brandon Benjamin Samuel Gregory Alexander Patrick Raymond Dennis Jerry Tyler Aaron Adam Nathan Henry Douglas Zachary Peter Kyle Ethan Walter Noah Jeremy Keith Roger Terry Gerald Harold Sean Carl Arthur Lawrence Dylan Jesse Bryan Bruce Gabriel Logan Albert Willie Alan Juan Wayne Elijah Randy Vincent Ralph Eugene Russell Bobby Philip Louis " +
        "Mary Patricia Jennifer Linda Elizabeth Barbara Susan Jessica Sarah Karen Lisa Nancy Betty Sandra Margaret Ashley Kimberly Emily Donna Michelle Carol Amanda Melissa Deborah Stephanie Rebecca Sharon Laura Cynthia Kathleen Amy Shirley Angela Helen Anna Brenda Pamela Nicole Samantha Katherine Emma Ruth Christine Catherine Debra Rachel Carolyn Janet Maria Heather Diane Julie Joyce Victoria Kelly Christina Lauren Joan Evelyn Olivia Judith Megan Cheryl Martha Andrea Frances Hannah Jacqueline Gloria Kathryn Alice Teresa Sara Janice Doris Madison Julia Denise Marilyn Beverly Danielle Theresa Sophia Marie Diana Brittany Natalie Isabella Charlotte Abigail Sophie Kayla Alexis Lori " +
        "Mike Jim Tom Dave Steve Chris Matt Dan Andy Tim Tony Ben Sam Jake Josh Alex Becky Jenny Jess Kate Katie Liz Beth Meg Maggie Molly Sally Tina Lucy Zoe Mia Ella Chloe Ava Nate Zack Cody Luke Owen Evan Connor Caleb Isaac Liam Lucas Oliver Jayden Mateo Sebastian Leo Harper Layla Nora Riley Zoey Penelope Stella Naomi Leah Lillian Audrey Maya Savannah " +
        "Carlos Luis Miguel Jorge Pedro Diego Javier Fernando Alejandro Roberto Ricardo Eduardo Manuel Antonio Francisco Sofia Isabel Carmen Lucia Elena Ana Marisol Guadalupe Gabriela Valentina Camila Daniela Mariana Fernanda Rodrigo Sergio Ramon Raul Hector Oscar Armando Gerardo Arturo Alberto Enrique Ernesto " +
        "Google Gmail YouTube Facebook Instagram WhatsApp Twitter Snapchat TikTok Pinterest Reddit LinkedIn Netflix Hulu Spotify Amazon Walmart Costco Starbucks McDonald's Microsoft Xbox PlayStation Nintendo Android iPhone iPad iPod iOS macOS Siri Alexa Samsung Firefox Skype Dropbox PayPal Venmo Lyft Tesla Toyota Chevrolet Disney Wikipedia Bluetooth WiFi"

    /** Words that are written with a capital only after the words that make them a date (the rest of the time they are ordinary words: "you may go", "a long march"). */
    private val DATE_WORDS = setOf("in", "on", "of", "by", "until", "since", "last", "next", "this", "early", "late", "mid", "during", "every", "from", "through", "till")
    private val CONTEXT: Map<String, Set<String>> = mapOf("may" to DATE_WORDS, "march" to DATE_WORDS)

    private val builtin: Map<String, String> by lazy {
        val m = HashMap<String, String>(2400)
        for (data in listOf(DATA_PLACES, DATA_NAMES)) for (w in data.split(' ')) if (w.isNotEmpty()) m.putIfAbsent(w.lowercase(), w)
        m
    }

    /**
     * The way [lower] is written wherever it stands: what this person's own writing says (they typed it with a capital at least twice and not mostly small: a name, a place; or they took the keyboard's capital back
     * more often than they kept it: small), else the built-in list, else (with [prev], the lowercase word before it) a date word that needs its context. Null = write it as it was typed.
     */
    fun form(lower: String, personal: PersonalModel?, prev: String? = null): String? {
        if (lower.length < 2) return null
        val learned = personal?.caseForm(lower)
        if (learned != null) return learned.takeIf { it.isNotEmpty() }              // "" = the person writes it small: no capital whatever the list says
        builtin[lower]?.let { return it }
        val after = CONTEXT[lower]
        if (after != null && prev != null && prev in after) return lower.replaceFirstChar { it.uppercase() }
        return null
    }

    /** True when the built-in list (or the context rule) has a special form for [lower] (the learner then also counts how often the person writes it small). */
    fun hasBuiltin(lower: String): Boolean = builtin.containsKey(lower) || CONTEXT.containsKey(lower)

    /** The number of built-in entries, for the tests. */
    internal fun builtinSize(): Int = builtin.size
}
