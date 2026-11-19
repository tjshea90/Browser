package app.kestrel.core.input.glide

/**
 * Everyday English, written by hand for measuring tap-typing word prediction (KB-406, owner request 17: "optimize ... word prediction accuracy"): the kind of thing a person types on a tablet or phone in a
 * day - texts to family and friends, notes to self, shopping and plans, short work messages, and web searches (typed lower case, no punctuation, into the browser's address box).
 * Nothing here is taken from a book, a site or a dataset: every line is original, so the corpus carries no licence and may live in the public repository. It is a YARDSTICK, never training data: the decoder and
 * the word list must not be tuned on it line by line (the numbers are compared between changes, and a change that only helps these lines is not a gain).
 * One sentence per line; the first word of a line starts a sentence (no word before it). Search lines are marked with a leading '>' and are typed without capitals or punctuation.
 */
object EvalCorpus {
    /** The sentences, each as the words in order, lower case, apostrophes kept ("don't"). */
    val sentences: List<List<String>> by lazy { parse(TEXT) }
    val searches: List<List<String>> by lazy { parse(SEARCHES) }

    fun parse(text: String): List<List<String>> =
        text.lines().map { it.trim().removePrefix(">").trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line -> line.lowercase().replace(Regex("[^a-z' ]"), " ").split(' ').map { it.trim('\'') }.filter { it.isNotEmpty() } }
            .filter { it.size >= 2 }

    private const val TEXT = """
# texts to family and friends
Hey are you coming over tonight
I'm on my way home right now
Can you pick up some milk on the way back
What time does the game start tomorrow
I will call you later this afternoon
Thanks for dinner it was really good
Did you see the message I sent this morning
We should go for a walk after lunch
I can't find my keys anywhere
Do you want me to bring anything
Happy birthday I hope you have a great day
Let me know when you get there
I'm going to be a little late
Are we still meeting at the park
That sounds like a good idea
I miss you so much already
Please don't forget to lock the door
The kids are playing in the garden
Mom said she will be here by six
I just got out of the shower
What are you doing this weekend
I think I left my phone at your place
Sorry I missed your call
I'll text you when I leave work
Can we talk about it tomorrow
It was so nice to see everyone yesterday
Do you need a ride to the airport
I love the picture you sent
Tell her I said hello
We are almost there
How was your day today
I'm so tired I could sleep for a week
Good morning I hope you slept well
Don't worry about it I'll take care of everything
Let's grab some coffee next week
I was just thinking about you
Where did you put the remote
Can you send me the address again
See you at the store in ten minutes
I have to take the dog to the vet
It's raining so we will stay inside
She called me last night and we talked for hours
I really appreciate all your help
Are you free on Friday evening
We had a great time at the beach
Remember to bring a jacket it's cold outside
Dinner is ready come downstairs
I'm sorry I forgot to call you back
That movie was so funny
My phone is about to die
Can you believe how fast this year went
I'll be home around eight
He said he would meet us at the restaurant
Thank you so much for the gift
I need to go to the bank before it closes
What do you want for breakfast
The train was late again this morning
Please call me as soon as you can
I hope everything is going well
We need to buy more groceries this week
I think it's going to snow tonight
Are you going to the party on Saturday
I wanted to say thank you for everything
Do you know what time the shop opens
It's been a long day and I'm ready for bed
Let's order pizza tonight
I'll see you on Monday
Can you help me move the couch
The weather is beautiful today
I'm still waiting for the package to arrive
We should plan a trip this summer
Please let me know if you need anything
I have a doctor appointment on Thursday
She is picking up the kids from school
That was the best meal I have had in a long time
I'm at the front door can you let me in
Did you finish the book I lent you
Sorry I'm running late traffic is terrible
I'll bring the cake if you bring the drinks
Why is the internet so slow today
I don't know what to cook for dinner
Just checking in to see how you are doing
Good luck on your test tomorrow
We are out of bread and eggs
Can you turn the music down a little
I'm proud of you for working so hard
What did the doctor say about your knee
Come over whenever you want
It's my turn to cook tonight
I can pick you up at seven
The meeting was moved to three o'clock
Please send me the report by the end of the day
I'll check my calendar and get back to you
Can we reschedule for next Tuesday
Thanks for letting me know
I'm working from home today
The client wants a few changes to the design
Let's discuss this in the morning
I will be out of the office until Monday
Could you review the document when you have time
We need more time to finish the project
I just sent the invoice to your email
The new update fixed most of the problems
Did you get a chance to look at my proposal
Let me look into it and I'll let you know
I'd like to schedule a call for later this week
The deadline is next Friday so we need to hurry
Please find the attached file
I think we should wait until we have more information
I'm not sure that is the best way to do it
We can talk about the budget during lunch
The printer is out of paper again
I need to update my password
The battery on my tablet doesn't last very long
How do I connect to the wifi in this building
My screen is frozen and I can't close the app
I'm trying to download the file but it keeps failing
The video won't play on my phone
Can you show me how to take a screenshot
I forgot my login information
It says there is no signal here
The page takes forever to load
Do you have a charger I can borrow
I'll send you the link in a minute
We watched a documentary about the ocean last night
The recipe says to bake it for forty minutes
I need to buy eggs butter flour and sugar
Add milk and bananas to the shopping list
There is a sale at the store this weekend
We ran out of paper towels
Pick up some tomatoes and onions please
The soup needs a little more salt
I made a big pot of chili for dinner
Do we have any cheese left in the fridge
I'm going to the gym after work
My back hurts from sitting all day
I have been trying to drink more water
We went for a long run this morning
I need to stretch before I go to bed
The game last night was incredible
Our team won by three points
I'm thinking about getting a new pair of shoes
The flight leaves at nine in the morning
We booked a room near the beach
Don't forget your passport and tickets
The hotel has a pool and a gym
It takes about two hours to drive there
We are planning to visit my parents next month
The bus stop is just around the corner
I'll meet you at the station
Remember to charge your phone before we leave
The cat knocked the plant off the shelf again
My brother is coming to visit for the holidays
We painted the living room last weekend
I need to clean the kitchen before they arrive
Have you started your homework yet
The teacher said the test would be easy
She learned how to ride a bike today
I think I'm getting a cold
Take some medicine and get some rest
The library closes at five today
I'm looking forward to the weekend
That was a really interesting article
I read somewhere that it might rain tomorrow
What a beautiful sunset
It's going to be a busy week
I can't wait to see you
Wish me luck
"""

    /**
     * Words people really mistype (typed -> meant): the usual lists of common spelling errors, written out from general knowledge. A space after the typed word should put the meant one in when nothing else could be meant.
     * One-slip errors (a letter left out / doubled / swapped / a neighbouring key) are the ones a keyboard can repair; the rest are listed too (they show what is out of reach).
     */
    val misspellings: List<Pair<String, String>> by lazy {
        MISSPELLINGS.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }.flatMap { l -> l.split(' ').map { it.substringBefore('>') to it.substringAfter('>') } }
    }
    private const val MISSPELLINGS = """
occured>occurred untill>until tommorow>tomorrow beleive>believe accomodate>accommodate adress>address begining>beginning comming>coming diffrent>different enviroment>environment
goverment>government happend>happened immediatly>immediately noticable>noticeable publically>publicly truely>truly writting>writing sucess>success succesful>successful neccessary>necessary
embarass>embarrass independant>independent knowlege>knowledge liason>liaison millenium>millennium occassion>occasion persistant>persistent posession>possession prefered>preferred
recomend>recommend refered>referred relevent>relevant rythm>rhythm seperate>separate definately>definitely freind>friend recieve>receive thier>their wierd>weird becuase>because
tomorow>tomorrow realy>really teh>the adn>and hte>the yeild>yield wich>which twelth>twelfth vaccum>vacuum whereever>wherever mispell>misspell acheive>achieve agressive>aggressive
apparantly>apparently arguement>argument calender>calendar catagory>category cemetary>cemetery collegue>colleague concious>conscious curiousity>curiosity dissapear>disappear
dissapoint>disappoint existance>existence experiance>experience foriegn>foreign fourty>forty gaurd>guard grammer>grammar hieght>height ignorence>ignorance intresting>interesting
lenght>length maintainance>maintenance medeval>medieval neice>niece ocurrence>occurrence oppurtunity>opportunity paralell>parallel parliment>parliament peice>piece plesant>pleasant
potatos>potatoes priviledge>privilege pronounciation>pronunciation reccomend>recommend religous>religious remeber>remember resistence>resistance responsability>responsibility
restaraunt>restaurant shedule>schedule sincerly>sincerely speach>speech strenght>strength suprise>surprise tendancy>tendency tommorrow>tomorrow unfortunatly>unfortunately
usualy>usually vegatable>vegetable whith>with wirting>writing yesturday>yesterday thanks>thanks thnks>thanks becasue>because thats>that's didnt>didn't cant>can't
"""

    /** Shorthand people type in messages: a keyboard must know it (never "correct" it into another word). */
    val abbreviations: List<String> by lazy { ABBREVIATIONS.trim().split(Regex("\\s+")) }
    private const val ABBREVIATIONS = """
pls plz thx thnx ty tysm omw idk imo imho btw fyi lmk tbh brb ttyl np yw rn irl nvm ikr ofc jk wtf lmao lmfao rofl hehe hahaha tho thru cuz gtg afk asap eta wfh dm tmrw sry srsly ppl abt
bday convo rly def prob ily ilu xoxo bff fam bro sis bruh yo sup wassup hbu hru wyd wya hmu kk ya yep yup nah nope ok okay hmm ugh oops yay omg gonna wanna gotta kinda sorta dunno lemme gimme lol haha
"""

    /** Real but uncommon words, typed correctly: a space must never change them into a common lookalike. */
    val rareWords: List<String> by lazy { RARE_WORDS.trim().split(Regex("\\s+")) }
    private const val RARE_WORDS = """
quixotic ephemeral obsequious sycophant ubiquitous mellifluous perfunctory pernicious recalcitrant magnanimous surreptitious inchoate pulchritude ineffable petrichor serendipity
defenestrate bloat float heron sheaf loath gloat sleet gazebo trowel sconce tarpaulin jodhpurs farrier bridle dressage fetlock withers pastern gelding stallion filly paddock kestrel falconry
tundra isthmus estuary fjord lagoon mesa plateau tarn glacier moraine crevasse scree cairn
"""

    private const val SEARCHES = """
> weather tomorrow
> best pizza near me
> how to reset a router
> how to change a flat tire
> what time does the pharmacy close
> cheap flights to london
> youtube relaxing music
> kotlin string split example
> how many ounces in a cup
> news today
> translate hello to spanish
> recipe for banana bread
> nearest gas station
> how to take a screenshot on android
> movie times near me
> what is the capital of australia
> how long to boil an egg
> best budget tablet
> weather this weekend
> how to remove a stain from a shirt
> football scores last night
> phone repair near me
> how to cook rice
> bus schedule downtown
> what is the time in tokyo
> how to make coffee without a machine
> home workout for beginners
> how far is the moon
> free ebooks to download
> how to speed up a slow tablet
> restaurants open late
> dog friendly parks
> how to write a cover letter
> convert miles to kilometers
> what does it mean when
> best way to learn guitar
> why is my wifi slow
> how to save battery on android
> new movies this month
> how to get a refund
"""
}
