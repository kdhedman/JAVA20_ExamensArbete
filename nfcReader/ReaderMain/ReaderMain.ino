#include <FlashStorage.h>
#include <ArduinoBLE.h>
//REDACTED
//ETHERNET
#include <SPI.h>        //Serial protocoll for Ethernet Shield
#include <Ethernet.h>   //For the Ethernet Shield components
#include <HttpClient.h> //For simpler code to do HTTP-requests
#include <MD5.h> 
//i2c-Slave
#include <Wire.h>

typedef struct{
  char tname[64] = "";
  char ttoken[64] = "";
} Token;

typedef struct {
  boolean set = false;
  boolean validated = false;
  uint8_t ip[4] = {0,0,0,0};
  uint8_t dns[4] = {0,0,0,0};
  uint8_t gateway[4]= {0,0,0,0};
  //uint8_t host[4] = {0,0,0,0};
  uint16_t port = 80;
  uint8_t subnet[4] = {0,0,0,0};;
  boolean dhcp = true;
  char ethUsername[32];
  char ethPassword[32];
  char host[32];

  void print(){
    Serial.println("Ethernet Settings values:");
    Serial.print("Set: ");
    Serial.println(set);
    Serial.print("Validated: ");
    Serial.println(validated);
    Serial.print("IP: ");
    for(uint8_t i = 0; i < sizeof(ip); i++){
      Serial.print(ip[i]);
      i == 3 ? Serial.println("") : Serial.print(".");
    }
    Serial.print("DNS: ");
    for(uint8_t i = 0; i < sizeof(dns); i++){
      Serial.print(dns[i]);
      i == 3 ? Serial.println("") : Serial.print(".");
    }
    Serial.print("Gateway: ");
    for(uint8_t i = 0; i < sizeof(gateway); i++){
      Serial.print(gateway[i]);
      i == 3 ? Serial.println("") : Serial.print(".");
    }
    Serial.print("Host: ");
    /*
    for(int i = 0; i < sizeof(gateway); i++){
      Serial.print(gateway[i]);
      Serial.print(".");
    }*/
    Serial.println(host);
    Serial.print("Port: ");
    Serial.println(port);
    Serial.print("Subnet: ");
    for(int i = 0; i < sizeof(subnet); i++){
      Serial.print(subnet[i]);
      i == 3 ? Serial.println("") : Serial.print(".");
    }
    Serial.print("DHCP: ");
    Serial.println(dhcp ? "true" : "false");
    Serial.print("User: ");
    Serial.println(ethUsername);
    Serial.print("Password: ");
    Serial.println(ethPassword);
  }
//  char *host;
} EthernetSettings;

/*************
 * PIN SETUP *
 *************/
//NFC
#define FD_PIN    0
//LED
#define LUCY_PIN  3
#define R_PIN     2
#define G_PIN     1
//Wiegand
#define W_D0      6
#define W_D1      7
//Maintenance Mode
#define MAINTENANCE_BUTTON A5

EthernetSettings ethSettings;
Token tokenIdPoint;
Token tokenDoor;
Token tokenDoorCtrl;

Token *availableIdPointTokens;
Token *availableDoorTokens;

FlashStorage(flash_store_eth, EthernetSettings);
FlashStorage(flash_store_token_idpoint, Token);
FlashStorage(flash_store_token_door, Token);
FlashStorage(flash_store_token_doorCtrl, Token);
FlashStorage(flash_store_maintenance_mode, bool);

boolean maintenanceMode = false;

long long whiteList[1000];

void setup() {
  ethSettings = flash_store_eth.read();
  tokenIdPoint = flash_store_token_idpoint.read();
  tokenDoor = flash_store_token_door.read();
  tokenDoorCtrl = flash_store_token_doorCtrl.read();
  maintenanceMode = flash_store_maintenance_mode.read();
  
  Serial.begin(115200);
  //while(!Serial){} 
  Serial.println("----------PROGRAM START----------");
  ethSettings.print();
  pinMode(MAINTENANCE_BUTTON, INPUT);
  if(!ethSettings.set){
    maintenanceMode = true;
    saveMaintenanceMode(false);
    bleSetup();
    bleStart();
    while(maintenanceMode){
      blePoll();
    }
  }
  nfcBegin();
  Wire.begin();
  ethSetup();
  setupPolarityLED();
  if(maintenanceMode){
    bleSetup();
    bleStart();
    ethRequestAccessCtrlTokens();
    
    while(maintenanceMode){
      blePoll();
      buttonPoll();
      LEDpulsate();   
    }
  }
}

String cardNumber = "";
long long cardNumberLong = 0L;

void loop() {
  buttonPoll();
  nfcPoll();
  if(cardNumber.length() > 1){
    if(ethRequestAccess(cardNumber)){
      sendCommandToSlave();
      LEDAccess();
    } else {
      LEDDenied();
    }
    cardNumber = "";
    cardNumberLong = 0L;
  }
  LEDLight();
}

void reboot(){
  NVIC_SystemReset();
}

EthernetSettings getEthSettings(){
  ethSettings = flash_store_eth.read();
}

void resetEthernetSettings(){
  EthernetSettings newSettings;
  flash_store_eth.write(newSettings);
  reboot();
}

void saveEthernetSettings(bool doReboot){
  flash_store_eth.write(ethSettings);
  ethSettings.print();
  if(doReboot){
    reboot();
  }
}

void saveMaintenanceMode(bool doReboot){
  flash_store_maintenance_mode.write(maintenanceMode);
  if(doReboot){
    reboot();
  }
}

void saveIdPointToken(){
  flash_store_token_idpoint.write(tokenIdPoint);
}

void saveDoorToken(){
  flash_store_token_door.write(tokenDoor);
}

void saveDoorCtrlToken(){
  flash_store_token_doorCtrl.write(tokenDoorCtrl);
}
