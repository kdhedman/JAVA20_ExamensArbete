//TODO implement heartbeat to/from slave to reboot if either component freezes.
char slaveCommands[] = {'a', 'b', 'c', 'd', 'e'};
int slaveLastIndex = 0;

//Test method to open each relay 
void sendCommandToSlave(){
  Serial.println("Requesting slave to open relay");
  Wire.beginTransmission(0x50);
  Wire.write(slaveCommands[slaveLastIndex]);
  Wire.write(';');
  Wire.endTransmission();
  slaveLastIndex = slaveLastIndex > 3 ? 0 : slaveLastIndex + 1; 
}

void sendCommandToSlave(char c){
  Serial.println("Requesting slave to open relay");
  Wire.beginTransmission(0x50);
  Wire.write(slaveCommands[slaveLastIndex]);
  Wire.endTransmission();
  slaveLastIndex = slaveLastIndex > 3 ? 0 : slaveLastIndex + 1; 
}
