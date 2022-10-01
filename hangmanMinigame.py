from random import randrange
import re

words = ["gnome", "ratio", "probably"]

victory = bool 

def makeWord():
    
    def getIntWordAsString():
      
      interfaceWordString = ""

      for i in interfaceWord:

        interfaceWordString += i

      return interfaceWordString 
    
    victory = False

    guessWord = words[randrange(0, 3)]

    guessWordArray = [letter for letter in guessWord]

    interfaceWord = [re.sub("[A-Za-z]", "_", underscore) for underscore in guessWordArray]

    matchIndex = []

    score = 0
    
    failCount = 0

    print("Your word is: " + getIntWordAsString())

    while not victory:

      guess = input("guess a letter: ")

      if guess.__len__() > 1:

       print("invalid input")

       continue

      index = 0

      curScore = score
       
      dupe = False 

      for check in guessWordArray:
        
        index += 1

        if guess == check:
        
          if index in matchIndex:
            
            dupe = True

            break
             
          interfaceWord[index - 1] = guess

          matchIndex.append(index)

          score += 1
          
      if curScore < score:

        print("nice, you got one")

      elif dupe:

        print("already guessed that one") 

        failCount += 1 
      else: 

        print("nope, try again")

        failCount += 1

      if score != guessWord.__len__():

        print(getIntWordAsString())

      else:
        
        print("you won, the word was: " + getIntWordAsString()) 

        victory = True 

      if failCount == 12:

        print("you got hung!!! ouch xD")

        break

print("Welcome to hangman!")

makeWord()