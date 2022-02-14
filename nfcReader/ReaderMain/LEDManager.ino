bool brighterled = true;
bool shinered = true;
int ledStrength = 250;
long ledTimer = millis();
long accessDuration = 4000L;
long accessTimer = 0L;
boolean access = false;
long deniedTimer = 0L;
long deniedDuration = 4000L;
boolean denied = false;


void LEDLight(){
  if(access && ((millis() - accessTimer) < accessDuration)){
    if((millis() - ledTimer) > 5){
        brighterled ? ledStrength++ : ledStrength--;
        if(ledStrength > 253){
          brighterled = false;
        } else if (ledStrength < 1){
          brighterled = true;
        }
        polarityGreen(ledStrength);
        ledTimer = millis();
      }
  } else if(denied && ((millis() - deniedTimer) < deniedDuration)){
    if((millis() - ledTimer) > 5){
        brighterled ? ledStrength++ : ledStrength--;
        if(ledStrength > 253){
          brighterled = false;
        } else if (ledStrength < 1){
          brighterled = true;
        }
        polarityRed(ledStrength);
        ledTimer = millis();
      }
  } else {
    if(access){access = false;}
    if(denied){denied = false;}
    if((millis() - ledTimer) > 5){
        ledStrength = 253;
        shinered ? polarityGreen(ledStrength) : polarityRed(ledStrength);
        shinered = !shinered;
        ledTimer = millis();
      }
  }
}

void LEDAccess(){
  access = true;
  accessTimer = millis();
}

void LEDDenied(){
  denied = true;
  deniedTimer = millis();
}

void LEDStrength(int strength){
    analogWrite(LUCY_PIN, strength);
}

void setupPolarityLED(){
  pinMode(R_PIN, OUTPUT);
  pinMode(G_PIN, OUTPUT);
  pinMode(LUCY_PIN, OUTPUT);
}


void LEDpulsate(){
  if((millis() - ledTimer) > 5){
        brighterled ? ledStrength++ : ledStrength--;
        if(ledStrength > 253){
          brighterled = false;
        } else if (ledStrength < 1){
          brighterled = true;
        }
        shinered ? polarityGreen(ledStrength) : polarityRed(ledStrength);
        shinered = !shinered;
        ledTimer = millis();
      }
}

void polarityGreen(int strength){
    strength = strength > 256 ? 255 : strength;
    polarityOff();
    LEDStrength(strength);
    digitalWrite(G_PIN, HIGH);
}
void polarityRed(int strength){
    strength = strength > 256 ? 255 : strength;
    polarityOff();
    LEDStrength(strength);
    digitalWrite(R_PIN, HIGH);
}
void polarityOff(){
    digitalWrite(R_PIN, LOW);
    digitalWrite(G_PIN, LOW);
}

void testPolarityLED(){
  polarityGreen(255);
  delay(1000);
  polarityRed(255);
  delay(1000);
  polarityOff();
}
