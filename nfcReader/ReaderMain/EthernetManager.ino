

char *USERNAME = "root";
char *PASSWORD = "pass";

char *HOST = "10.144.11.55"; //Server to send request to
//char *URI = "/vapix/pacs"; //URI for specific request

//TODO: Needs to be unique for every reader.
byte MAC[] = {0xA8, 0x61, 0x0A, 0xAE, 0x54, 0xBE};
EthernetClient ethcl;
/******************
 * ETHERNET SETUP *
 ******************/
void ethSetup(){
  randomSeed(random(0,9999999));
  Ethernet.init(5);   // MKR ETH shield
  Serial.println("ETH: Initialize Ethernet with DHCP:");
  if (Ethernet.begin(MAC) == 0) {
    Serial.println("ETH: Failed to configure Ethernet using DHCP");
    // Check for Ethernet hardware present
    if (Ethernet.hardwareStatus() == EthernetNoHardware) {
      Serial.println("ETH: Ethernet shield was not found.  Sorry, can't run without hardware. :(");
      while (true) {
        delay(1); // do nothing, no point running without Ethernet hardware
      }
    }
    if (Ethernet.linkStatus() == LinkOFF) {
      Serial.println("ETH: Ethernet cable is not connected.");
    }
    // try to congifure using IP address instead of DHCP:
    IPAddress ip(ethSettings.ip[0],ethSettings.ip[1],ethSettings.ip[2],ethSettings.ip[3]);
    IPAddress dns(ethSettings.dns[0],ethSettings.dns[1],ethSettings.dns[2],ethSettings.dns[3]);
    Ethernet.begin(MAC, ip, dns);
    //Ethernet.begin(MAC, ip, dns, gateway, subnet)
  } else {
    Serial.print("  DHCP assigned IP ");
    Serial.println(Ethernet.localIP());
  }
  // give the Ethernet shield a second to initialize:
  delay(1000);
}

/**************************************
 * AXIS API REQUESTS
 ******************************/

boolean ethRequestAccess(String& cardNr){
  String postData = //REDACTED
  Serial.print("ETH: Requesting open door w data: ");
  Serial.println(postData);
  String response = ethPostRequest(postData, //REDACTED);
  //if(respone.equals("0")){
  //  return false;
  //} else {
    int start = response.indexOf("AccessGranted");
    bool access = "";
    String reason = "";
    if(start > 0){
      String accessTemp = response.substring(start + 16, start + 21);
      if(accessTemp.substring(0,4).equals("true")){
        return true;
      }
    }
  //  }
  return false;
}

boolean ethAccessDoor(){
  String postData = //REDACTED
  String response = ethPostRequest(postData, //REDACTED);
  Serial.println("ETH: trying to access door... : ");
  //TODO: Make a better check.
  if(response.length() < 4){
    Serial.println("Success!");
    Serial.println(response);
    return true;
  }
  Serial.println("Failed... :");
  Serial.println(response);
  return false;
}

boolean ethRequestIdPoints(){
  String postData = //REDACTED
  String response = ethPostRequest(postData, //REDACTED);
  Serial.println("ETH: Requesting ID-Point Tokens... : ");
  Token idPointTokens[32];
  Serial.println("      Id Points: ");
  uint8_t amountOfTokens = extractTokens(response, idPointTokens);
  if(amountOfTokens > 0){
    availableIdPointTokens = idPointTokens;
    return true;
  } 
  Serial.println("      No tokens found");
  return false;
}



boolean ethRequestDoorTokens(){
  String postData = "{\"axtdc:GetDoorList\":{}}"; 
  String response = ethPostRequest(postData, //REDACTED);
  Serial.println("ETH: Requesting ID-Point Tokens... : ");
  Serial.println("      Door Tokens: ");
  Token *doorTokens = new Token[16];
  uint8_t amountOfTokens = extractTokens(response, doorTokens);
  response = "";
  postData = "";
  availableDoorTokens = doorTokens;
  if(amountOfTokens > 0){
    return true;
  }
  
  Serial.println("      No tokens found");
  return false;
}

boolean ethRequestAccessCtrlTokens(){
  String postData = //REDACTED
  String response = ethPostRequest(postData, //REDACTED);
  Serial.println("ETH: Request Access Ctrl Tokens: ");
  Token doorCtrlTokens[2];
  uint8_t amountOfTokens = extractTokens(response, doorCtrlTokens);
  if(amountOfTokens > 0){
    tokenDoorCtrl = doorCtrlTokens[0];
    saveDoorCtrlToken();
    Serial.println("      Door Control Token Saved");
    return true;
  } 
  Serial.println("     No tokens found");
  return false;
}

uint8_t extractTokens(String& input, Token *out){
  uint8_t index = 0;
  int16_t lastFoundStart = 0;
  String tokenToken = "";
  String tokenName = "";
  while(input.indexOf("token", lastFoundStart) > 0){
    int tokenStart = input.indexOf("\"token\": ", lastFoundStart);
    int tokenEnd = input.indexOf("\",\n", tokenStart);
    tokenToken = input.substring(tokenStart+10, tokenEnd);

    int nameStart = input.indexOf("\"Name\":", lastFoundStart);
    int nameEnd = input.indexOf("\",\n", nameStart);
    tokenName = input.substring(nameStart+9, nameEnd);
    strcpy(out[index].ttoken, tokenToken.c_str());
    strcpy(out[index].tname, tokenName.c_str());
    
    lastFoundStart = nameStart + 10;
    index++;

    Serial.print("Token: ");
    Serial.println(tokenToken);
    Serial.print("Token Name: ");
    Serial.println(tokenName);
  }
  return index;
}

/**************************************
 * POST REQEUST SCRIPT
 **************************************/

String ethPostRequest(String& postData, char *URI){
  HttpClient client = HttpClient(ethcl, ethSettings.host, ethSettings.port);
  Serial.println("ETH: Making GET-request, expecting 401. ");
  client.get(URI);
  Serial.print("ResponseStatusCode: ");
  int responseCode = client.responseStatusCode();
  Serial.println(responseCode);
  if(responseCode == 401){  //First contact with Digest Authentication should be 401, to get the server nonce.
    String authReq = "default";
    while(client.headerAvailable()){
      if(client.readHeaderName().compareTo("WWW-Authenticate") == 0){
        authReq = client.readHeaderValue();
        break;
      } 
    }

    String authorization = getDigestAuth(authReq, String(ethSettings.ethUsername), String(ethSettings.ethPassword), "POST", String(URI), 1);
    
    int postData_length = postData.length();
    byte postDataBytes[postData_length + 1];
    postData.getBytes(postDataBytes, postData_length+1);
    
    client.beginRequest();
    client.post(URI);
    client.sendHeader(HTTP_HEADER_CONTENT_TYPE, "application/json");
    client.sendHeader(HTTP_HEADER_CONTENT_LENGTH, postData.length());
    client.sendHeader("Authorization", authorization);
    client.endRequest();
    client.print(postData);
    int statusCode = client.responseStatusCode();
    if(statusCode == 200){
      return client.responseBody();
    } else {
      String result = "0";
      return result;
    }
  } else {
    Serial.println("ETH: Request failed");
  }
}



/****************************
 * DIGEST RELATED FUNCTIONS *
 ****************************/
String getDigestAuth(String& authReq, const String& username, const String& password, const String& method, const String& uri, unsigned int counter) {
  // extracting required parameters for RFC 2069 simpler Digest
  String realm = exractParam(authReq, "realm=\"", '"');
  String nonce = exractParam(authReq, "nonce=\"", '"');
  String cNonce = getCNonce(8);

  char nc[9];
  snprintf(nc, sizeof(nc), "%08x", counter);
  
  //parameters for the RFC 2617 newer Digest
  //HA1
  String sBuffer = username + ":" + realm + ":" + password;
  int sBufferLength = sBuffer.length() + 1;
  char userRealmPass[sBufferLength];
  sBuffer.toCharArray(userRealmPass, sBufferLength);
  unsigned char* a1Hash = MD5::make_hash(userRealmPass);
  char* a1Md5Str = MD5::make_digest(a1Hash, 16);

  //HA2
  sBuffer = method + ":" + uri;
  sBufferLength = sBuffer.length() + 1;
  char methodURI[sBufferLength];
  sBuffer.toCharArray(methodURI, sBufferLength);

  unsigned char* a2Hash = MD5::make_hash(methodURI);
  char* a2Md5Str = MD5::make_digest(a2Hash, 16);
    
  //response
  sBuffer = String(a1Md5Str) + ":" + nonce + ":" + nc + ":" + cNonce + ":" + "auth" + ":" + String(a2Md5Str);
  sBufferLength = sBuffer.length() + 1;
  char a1Stuffa2[sBufferLength];
  sBuffer.toCharArray(a1Stuffa2, sBufferLength);

  unsigned char* responseHash = MD5::make_hash(a1Stuffa2);
  char* responseMd5Str = MD5::make_digest(responseHash, 16);
  String authorization = "Digest username=\"" + username + "\", realm=\"" + realm + "\", nonce=\"" + nonce +
                         "\", uri=\"" + uri + "\", algorithm=\"MD5\", qop=\"auth\", nc=\"" + String(nc) + "\", cnonce=\"" + cNonce + "\", response=\"" + String(responseMd5Str) + "\"";
  return authorization;
}

String getCNonce(const int len) {
  static const char alphanum[] =
    "0123456789"
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz";
  String s = "";
  for (int i = 0; i < len; ++i) {
    s += alphanum[rand() % (sizeof(alphanum) - 1)];
  }
  return s;
}

String exractParam(String& authReq, const String& param, const char delimit) {
  int _begin = authReq.indexOf(param);
  if (_begin == -1) {
    return "";
  }
  return authReq.substring(_begin + param.length(), authReq.indexOf(delimit, _begin + param.length()));
}
