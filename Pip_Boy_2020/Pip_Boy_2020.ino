#include <SPI.h>
#include <Adb.h>

// Adb connection.
Connection * connection;

// Elapsed time for ADC sampling
long lastTime;

// TA7291P
int mirrorBall = 3;
int clockwise = 2;
int counterClockwise = 4;

// LED
int led = 6;

// Event handler for the shell connection. 
void adbEventHandler(Connection * connection, adb_eventType event, uint16_t length, uint8_t * data)
{
  // Data packets contain two bytes, [motor, led]
  if (event == ADB_CONNECTION_RECEIVE)
  {
    // Motor
    if (data[0] < 127) {
      analogWrite(mirrorBall, (127 - data[0]) * 2 + 1);
      digitalWrite(clockwise, LOW);
      digitalWrite(counterClockwise, HIGH);
    } else if (data[0] > 128) {
      analogWrite(mirrorBall, (data[0] - 128) * 2 + 1);
      digitalWrite(clockwise, HIGH);
      digitalWrite(counterClockwise, LOW);
    } else {
      digitalWrite(clockwise, LOW);
      digitalWrite(counterClockwise, LOW);
    }
  
    // LED
    analogWrite(led, data[1]);
  }

}

void setup()
{
  // Initialise serial port
  Serial.begin(57600);
  
  // Note start time
  lastTime = millis();
  
  // Initialise the ADB subsystem.  
  ADB::init();

  // Open an ADB stream to the phone's shell. Auto-reconnect
  connection = ADB::addConnection("tcp:4567", true, adbEventHandler); 
  
  // TA7291P
  pinMode(mirrorBall, OUTPUT);
  pinMode(clockwise, OUTPUT);
  pinMode(counterClockwise, OUTPUT);  
  // Default stop
  digitalWrite(clockwise, LOW);
  digitalWrite(counterClockwise, LOW);
  
  // LED
  pinMode(led, OUTPUT);
}

void loop()
{
  // Polling for sensors
  if ((millis() - lastTime) > 1000)
  {
    uint16_t data = analogRead(A0);
    connection->write(2, (uint8_t*)&data);
    lastTime = millis();
  }

  // Poll the ADB subsystem.
  ADB::poll();
}

