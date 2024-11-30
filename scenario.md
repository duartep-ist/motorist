# MotorIST
IST is now selling electric cars with modern management systems. The system allows users to configure the car remotely, such as close/open the car; configure the AC; check the battery level. This is done by a user application installer on the users computer/mobile.
Additionally, and to maintain the car up to date, the car also allows for firmware updates from the manufacturer.

The communication with the car is done using a JSON document with the following data structure:

```json
{
 "carID": "1234XYZ",
 "user": "user1",
 "configuration": {
	"ac": [
   	{ "out1": "789"},
   	{ "out2": "1011"}
	],
	"seat": [
   	{ "pos1": "0"},
   	{ "pos3": "6"}
	]
 }
}
```

## Protection needs
All the communication with the car is sensitive and to assure the drivers and owner data protection according to the RGPD (GDPR) must be secure. External entities cannot change or check the user configurations and only the car manufacturer can update the car firmware. The mechanic cannot see the user configurations (unless he has the key) and can only update the car with firmware authorized by the manufacturer.

The following security requirements must be met:
- [SR1: Confidentiality] The car configurations can only be seen by the car owner.
- [SR2: Integrity 1] The car can only accept configurations sent by the car owner.
- [SR3: Integrity 2] The car firmware updates can only be sent by the car manufacturer.
- [SR4: Authentication] The car manufacture cannot deny having sent firmware updates.


## Security challenges

### Security challenge A
To facilitate the customization and improve the user experience multiple users/drivers exist. They are identified by their unique keys.

Ensure the following security requirements are met:
- [SRA1: data privacy] One user cannot know the configuration of the other user, but may know some current information of the car. For example, user 1 may not know how many km were done by the previous user, but can see the remaining battery level.
- [SRA2: authorization] An unauthorized user cannot change the configuration of the other user.
- [SRA3: authenticity] It must be possible to audit the car and verify which configuration actions were performed by which users.

### Security challenge B
The car must have a maintenance mode, which is set by the user. In this mode the car is set to the default configuration.

Ensure the following security requirements are met:
- [SRB1: data privacy] The mechanic cannot see the user configurations, even when he has the car key.
- [SRB2: authorization] The mechanic (when authenticated) can change any parameter of the car, for testing purposes. 
- [SRB3: data authenticity] The user can verify that the mechanic performed all the tests to the car. 
