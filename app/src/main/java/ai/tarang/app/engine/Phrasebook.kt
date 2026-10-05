package ai.tarang.app.engine

/** Everyday phrases, written in English and translated on the fly into any language pair. */
object Phrasebook {
    data class Category(val title: String, val emoji: String, val phrases: List<String>)

    val categories = listOf(
        Category("Greetings", "🙏", listOf(
            "Hello, how are you?",
            "Nice to meet you.",
            "What is your name?",
            "My name is …",
            "Thank you very much.",
            "Please speak slowly.",
            "I don't understand.",
            "Goodbye, take care.",
        )),
        Category("Travel", "🛺", listOf(
            "Where is the railway station?",
            "How far is it from here?",
            "Please take me to this address.",
            "How much is the fare?",
            "Please use the meter.",
            "Stop here, please.",
            "Which bus goes to the city centre?",
            "Is this the right platform?",
        )),
        Category("Food", "🍛", listOf(
            "A table for two, please.",
            "What do you recommend?",
            "I am vegetarian.",
            "Not too spicy, please.",
            "Can I have some water?",
            "The food was delicious.",
            "Please bring the bill.",
            "Do you accept UPI?",
        )),
        Category("Shopping", "🛍️", listOf(
            "How much does this cost?",
            "That is too expensive.",
            "Can you give me a discount?",
            "Do you have a smaller size?",
            "I'll take this one.",
            "Can I pay by card?",
            "Please give me a bag.",
            "When do you close today?",
        )),
        Category("Health", "🩺", listOf(
            "I need a doctor.",
            "Where is the nearest pharmacy?",
            "I have a fever.",
            "I am allergic to this medicine.",
            "My stomach hurts.",
            "Please call an ambulance.",
            "How many times a day should I take this?",
            "Is the hospital open now?",
        )),
        Category("Emergency", "🚨", listOf(
            "Help!",
            "Please call the police.",
            "I am lost.",
            "I lost my phone.",
            "Please help me find this place.",
            "Is it safe here?",
            "Can I use your phone?",
            "Please contact my family.",
        )),
    )
}
