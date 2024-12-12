# Threat model

Capabilities of attackers:

- An attacker can intercept and modify all communications between the application and the car
- An attacker (including any user) can access the car's stored data only while it is powered off.
- An attacker cannot access the users' keys nor guess the users' passwords.

Note: The car must know the user configurations in order to apply them, so it's impossible to design a secure system if we assume that the car may be compromised at any time.

Note: If the attacker couldn't access the data while the car is powered off, it wouldn't be necessary to encrypt data before storing it.
