
long pressStart = 0L;
void buttonPoll(){
  if(digitalRead(MAINTENANCE_BUTTON)){
    if(pressStart - millis() > 100){
      maintenanceMode = !maintenanceMode;
      saveMaintenanceMode(true);
    } else if ((pressStart - millis()) >  250 || pressStart == 0){
      pressStart = millis();
    }
  }
}
