//This section normally conatains more code, is now heavily redacted.
//REDACTED

void nfcBegin(){
//REDACTED
}

void nfcPoll(){
//REDACTED
}

//REDACTED

void handleNFCData(//REDACTED){
    //REDACTED
    long long payloadLong = 0;
    for (int c = 0; c < payloadAsString.length(); ++c) {
      char character = payloadAsString.charAt(c);
      payloadLong = payloadLong*10 + character - '0';
    }

    if(payloadLong > 0){
       cardNumber = payloadAsString;
       cardNumberLong = payloadLong;
    }
}

long long char2LL(char *str){
  long long result = 0;  
  // Iterate through all characters of input string and update result   
  for (int i = 0; str[i] != '\0'; ++i) result = result*10 + str[i] - '0';
  return result; 
} 
void LL2Serial(long long ll) {
  uint64_t xx = ll/1000000000ULL;
  if (xx >0) Serial.print((long)xx);
  Serial.print((long)(ll-xx*1000000000)); 
}
