#include <Wire.h>

byte address = 0x50;

#define RELAY1 5
#define RELAY2 2
#define RELAY3 3
#define RELAY4 4
#define BUZZER1 6

long durationRelay1;
long durationRelay2;
long durationRelay3;
long durationRelay4;
long durationBuzzer;

bool relay1 = false;
bool relay2 = false;
bool relay3 = false;
bool relay4 = false;
bool buzzer = false;

void setup()
{
  pinMode(RELAY1, OUTPUT);
  digitalWrite(RELAY1, HIGH);
  pinMode(RELAY2, OUTPUT);
  digitalWrite(RELAY2, HIGH);
  pinMode(RELAY3, OUTPUT);
  digitalWrite(RELAY3, HIGH);
  pinMode(RELAY4, OUTPUT);
  digitalWrite(RELAY4, HIGH);
  pinMode(BUZZER1, OUTPUT);
  digitalWrite(BUZZER1, HIGH);
  digitalWrite(BUZZER1, LOW);
  
  Wire.begin(address);                // join i2c bus with address 0x50
  Wire.onReceive(receiveEvent); // register event
  //Serial.begin(115200);           // start serial for output
  //Serial.println(F("Program Start -- Uno Slave"));

  //TODO: Make Request do this
  durationRelay1 = 4000;
  durationRelay2 = 4000;
  durationRelay3 = 4000;
  durationRelay4 = 4000;
  durationBuzzer = 1000;
}
long timerRelay1 = 0L;
long timerRelay2 = 0L;
long timerRelay3 = 0L;
long timerRelay4 = 0L;
long timerBuzzer = 0L;
void loop()
{
  if(relay1 && ((millis() - timerRelay1) < durationRelay1)){
    digitalWrite(RELAY1, LOW);
  } else {
    digitalWrite(RELAY1, HIGH);
    relay1 = false;
  }
  if(relay2 && ((millis() - timerRelay2) < durationRelay2)){
    digitalWrite(RELAY2, LOW);
  } else {
    digitalWrite(RELAY2, HIGH);
    relay2 = false;
  }
  if(relay3 && ((millis() - timerRelay3) < durationRelay3)){
    digitalWrite(RELAY3, LOW);
  } else {
    digitalWrite(RELAY3, HIGH);
    relay3 = false;
  }
  if(relay4 && ((millis() - timerRelay4) < durationRelay4)){
    digitalWrite(RELAY4, LOW);
  } else {
    digitalWrite(RELAY4, HIGH);
    relay4 = false;
  }
    if(buzzer && ((millis() - timerBuzzer) < durationBuzzer)){
    digitalWrite(BUZZER1, HIGH);
  } else {
    digitalWrite(BUZZER1, LOW);
    buzzer = false;
  }  
}

void receiveEvent(int howMany)
{
  //Serial.print(F("Recieving: "));
  while(1 < Wire.available()) // loop through all but the last
  {
    char c = Wire.read(); // receive byte as a character
    //Serial.print(c);         // print the character
    if(c == 'a'){
      timerRelay1 = millis();
      relay1 = true;
    } else if(c == 'b'){
      timerRelay2 = millis();
      relay2 = true;
    } else if(c == 'c'){
      timerRelay3 = millis();
      relay3 = true;
    } if(c == 'd'){
      timerRelay4 = millis();
      relay4 = true;
    } else if(c == 'e'){
      timerBuzzer = millis();
      buzzer = true;
    }
  }
  Wire.read();    // receive byte as an integer
}
