//TODO: Control UUID-Standards
BLEService ledService("4bb31300-1936-4fc5-9816-8d1d18d85f81"); 
BLEService tokenService("4bb31400-1936-4fc5-9816-8d1d18d85f81");
BLEService choiceService("4bb31500-1936-4fc5-9816-8d1d18d85f81");

BLEStringCharacteristic infoCharacteristic("4bb31501-1936-4fc5-9816-8d1d18d85f81", BLERead | BLENotify, 128);
BLEDescriptor infoDescriptor("2ABE", "Info");
BLEStringCharacteristic inputStringCharacteristic("4bb31504-1936-4fc5-9816-8d1d18d85f81", BLEWrite, 128);

const char BLE_EXIT_CONFIG_MODE = '1';
const char BLE_IP_CONFIG = '2';
const char BLE_IP_RESET = '3';
const char BLE_UPDATE_IDPOINT = '4';
const char BLE_IDPOINT_CHOICE = '5';
const char BLE_UPDATE_DOORS = '6';
const char BLE_DOOR_CHOICE = '7';
const char BLE_ACCESS_DOOR = '8';

const char BLEDELIMITER = ',';

void bleSetup(){
  if(!BLE.begin()){
    Serial.println("BLE: failed to initiate.");
  }
  BLE.setLocalName("NFC-EntryfyPad");
  BLE.setAdvertisedService(ledService);
  infoCharacteristic.addDescriptor(infoDescriptor);
  choiceService.addCharacteristic(inputStringCharacteristic);
  choiceService.addCharacteristic(infoCharacteristic);
  BLE.addService(choiceService);
  BLE.addService(tokenService);
  BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);

  inputStringCharacteristic.setEventHandler(BLEWritten, inputStringCharacteristicWritten);  
}

void bleUpdateIDPoints(){
  String chrval = "";
  for(int i = 0; i < sizeof(availableIdPointTokens); i++){
    chrval += String(availableIdPointTokens[i].tname);
    if(i != sizeof(availableIdPointTokens) - 1){
      chrval += ";";
    }
  }
  chrval = "" + String(BLE_UPDATE_IDPOINT) + ":" + chrval;
  infoCharacteristic.setValue(chrval);
  Serial.print("BLE: Updated IDPOINTs with: ");
  Serial.println(chrval);
}

void bleUpdateDoors(){
  String chrval = "";
  for(int i = 0; i < sizeof(availableDoorTokens); i++){
    if(isAlphaNumeric(availableDoorTokens[i].tname[0])){
      chrval += String(availableDoorTokens[i].tname);
      if(i != sizeof(availableDoorTokens) - 1){
        chrval += ";";
      }
    }
  }
  chrval = "" + String(BLE_UPDATE_DOORS) + ":" + chrval;
  infoCharacteristic.setValue(chrval);
  Serial.print("Updated Targets/Doors with: ");
  Serial.println(chrval);
}

boolean bleSaveDoorToken(String input){
  input.remove(0,1);
  for(int i = 0; i < sizeof(availableDoorTokens); i++){
    if(isAlphaNumeric(availableDoorTokens[i].tname[0])){
      for(int j = 0; j < input.length(); j++){
        if(input.charAt(i+j) == availableDoorTokens[i].tname[j]){
          if(j == input.length() - 1){
            Serial.print("BLE: Found matching Token: ");
            Serial.print(availableDoorTokens[i].tname);
            Serial.print(", ");
            Serial.println(availableDoorTokens[i].ttoken);
            memset(tokenDoor.tname, 0, sizeof(tokenDoor.tname));
            memset(tokenDoor.ttoken, 0, sizeof(tokenDoor.ttoken));
            memcpy(tokenDoor.tname, availableDoorTokens[i].tname, sizeof(availableDoorTokens[i].tname));
            memcpy(tokenDoor.ttoken, availableDoorTokens[i].ttoken, sizeof(availableDoorTokens[i].ttoken));
            saveDoorToken();
            Serial.println("BLE: Door token set.");
            return true;
          }
        } else {
          break;
        }
      }
    }
  }
  Serial.println("BLE: Found no matching Token:");
}

boolean bleSaveIdPointToken(String input){
  input.remove(0,1);
  for(int i = 0; i < sizeof(availableIdPointTokens); i++){
    if(isAlphaNumeric(availableIdPointTokens[i].tname[0])){
      for(int j = 0; j < input.length(); j++){
        if(input.charAt(i+j) == availableIdPointTokens[i].tname[j]){
          if(j == input.length() - 1){
            Serial.print("BLE: Found matching Token: ");
            Serial.print(availableIdPointTokens[i].tname);
            Serial.print(", ");
            Serial.println(availableIdPointTokens[i].ttoken);
            memset(tokenIdPoint.tname, 0, sizeof(tokenIdPoint.tname));
            memset(tokenIdPoint.ttoken, 0, sizeof(tokenIdPoint.ttoken));
            memcpy(tokenIdPoint.tname, availableIdPointTokens[i].tname, sizeof(availableIdPointTokens[i].tname));
            memcpy(tokenIdPoint.ttoken, availableIdPointTokens[i].ttoken, sizeof(availableIdPointTokens[i].ttoken));
            saveIdPointToken();
            Serial.println("BLE: IDPoint token set.");
            return true;
          }
        } else {
          break;
        }
      }
    }
  }
  Serial.println("BLE: Found no matching Token:");  
}


void blePoll(){
  BLE.poll();
}

void bleStart(){
  BLE.advertise();
}

void bleStop(BLEDevice central){
  if(central.connected()){
    central.disconnect();
  }
  BLE.end();
}

void blePeripheralConnectHandler(BLEDevice central) {
  Serial.print("BLE: Connected event, central: ");
  Serial.println(central.address());
}

void blePeripheralDisconnectHandler(BLEDevice central) {
  Serial.print("BLE: Disconnected event, central: ");
  Serial.println(central.address());
}

void inputStringCharacteristicWritten(BLEDevice central, BLECharacteristic characteristic){
  Serial.print("BLE: Choice value: ");
  Serial.println(inputStringCharacteristic.value());
  char choice = inputStringCharacteristic.value().charAt(0);
 
  if(choice == BLE_EXIT_CONFIG_MODE){          //IF DONE WITH BLUETOOTH, call for reboot
    maintenanceMode = false;
    saveMaintenanceMode(true);
  } else if(choice == BLE_IP_CONFIG) {   //TRY TO READ VALUES
    extractValues(inputStringCharacteristic.value());
  } else if(choice == BLE_IP_RESET){   // RESET IP and user VALUES
    resetEthernetSettings();
  } else if(choice == BLE_UPDATE_IDPOINT){   // UPDATE IDPOINT TOKENS
     ethRequestIdPoints();
     bleUpdateIDPoints();
  } else if(choice == BLE_IDPOINT_CHOICE){   // IDPOINT TOKEN CHOICE
     bleSaveIdPointToken(inputStringCharacteristic.value());
  } else if(choice == BLE_UPDATE_DOORS){   // UPDATE DOOR TOKENS
     ethRequestDoorTokens();
     bleUpdateDoors();
  } else if(choice == BLE_DOOR_CHOICE){   // DOOR TOKEN CHOICE
     bleSaveDoorToken(inputStringCharacteristic.value());
  } else if(choice == BLE_ACCESS_DOOR){   //ELSE IF TEST OPEN DOOR
    //ethAccessDoor();
    String cardNr = "3503704636";
    ethRequestAccess(cardNr);
  }
}

uint8_t getValueLength(uint8_t startPos, String &source){
  uint8_t steps;
  if(source.charAt(startPos + 1) == ':'){
    steps = 2;
    while(source.charAt(startPos+steps) != ','){
      steps++;
    }
  }
  int result = steps - 2;
  return result;
}

void extractIP(char *in, uint8_t *out){
  uint8_t steps = 3;
  uint8_t pow10 = 1;
  char values[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  for(int k = strlen(in); k >= 0; k--){
    if(in[k] == '.'){
      steps--;
      pow10 = 1;
    } else {
      for(int j = 0; j < sizeof(values); j++){
        if(in[k] == values[j]){
          out[steps] = out[steps] + (j * pow10);
          pow10 = pow10 * 10;
        }
      }
    }
  }
}

uint16_t extractPort(char *in){
  uint16_t pow10 = 1;  
  char values[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  uint16_t result = 0;
  for(int i = strlen(in); i >= 0; i--){
    for(int j = 0; j < sizeof(values); j++){
      if(values[j] == in[i]){
        result = result + (j * pow10);
        pow10 = pow10 * 10;
      }
    }
  }
  return result;
}

void extractValues(String source){
  char buffer[64];
  memset(buffer, 0, sizeof(buffer));
  int skip = 0;
  boolean ending = true;
  char _DELIMITER = '|';
 
  for(int i = 0; i < source.length(); i++){
    if(skip > 0){
      skip--;
    } else {
      char c = source.charAt(i);
      if((c == _DELIMITER) && (ending == true)) {
        Serial.println("BLE: parsing completed");
        break;
      }
      if(c == _DELIMITER){
        ending = true;
      } else if (c == 'u'){ //U for User
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          strcpy(ethSettings.ethUsername, temp);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'w'){ //Wee for password
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          strcpy(ethSettings.ethPassword, temp);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'i'){ //I for IP
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          extractIP(_ip, ethSettings.ip);       
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'c'){ //C for DH_C_P
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            _dhcp = buffer[bufferIndex] == '1' ? true : false;
            bufferIndex--;
          }
          ethSettings.dhcp = (_dhcp ? true : false);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'd'){ //D for DNS
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          extractIP(temp, ethSettings.dns);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'g'){ //G for Gateway
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          extractIP(temp, ethSettings.gateway);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'h'){ //H for Host, Door Ctrl IP:Port
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          strcpy(ethSettings.host, temp);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 'p'){ //P for port, Door Ctrl IP:Port
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          ethSettings.port = extractPort(temp);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      } else if(c == 's'){ //S for Subnet
        if(source.charAt(i+1) == ':'){
          skip = 1;
          int bufferIndex = 2;
            while(source.charAt(i+bufferIndex) != _DELIMITER){
              buffer[bufferIndex-2] = source.charAt(i+bufferIndex);
              bufferIndex++;
              skip++;
            }
          char temp[bufferIndex - 2];
          bufferIndex--;
          bufferIndex--;
          while(bufferIndex >= 0){
            temp[bufferIndex] = buffer[bufferIndex];
            bufferIndex--;
          }
          extractIP(temp, ethSettings.subnet);
        }
        memset(buffer, 0, sizeof(buffer));
        ending = false;
      }
    }
  }
  ethSettings.set = true;
  saveEthernetSettings(true);
}
