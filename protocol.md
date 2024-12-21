# Car control protocol

The car control protocol is used by the application to communicate with the car. The car acts as a server and the app acts as a client. The messages exchanged are JSON values followed by a line feed character (`\n`) encoded with UTF-8.

**TODO:** Errors.

## User authentication handshake

These are the first steps to connect to the car. No messages should be sent by the app before the **authentication request message**.

1. The app, knowing the car's public key (SSH) or knowing the public key for the car's root certificate (TLS) establishes a secure channel with the car.
2. The app sends an **authentication request message** containing the username and the password.
3. The car sends an **authentication challenge message** containing random data.
4. The app sends an **authentication proof message** containing a MAC of the challenge. This proves to the car that the client who is connecting is in possession of the user key.
5. The car tries to decrypt the stored encrypted user key for the user with the given username using the password.
6. If the password was wrong or the MAC is invalid, the car sends an **authentication failure message**.
7. If not, the car sends a **authentication confirmation message** and all further requests from this connection are trusted as coming from the client.

**Note:** An attacker cannot send messages in the connection after the handshake because it's a secure channel and all messages are signed by the session key. The client must keep this key secret.

**Note:** Deferring the password check until the **authentication proof** has been received prevents an attacker from brute-forcing the password without the user key.

### Authentication request message (app → car)

JSON object containing:
- `type`: `"AUTH_REQUEST"`
- `username`: username (string)
- `password`: password (string)

### Authentication challenge message (car → app)

JSON object containing:
- `type`: `"AUTH_CHALLENGE"`
- `challenge`: BytesToBase64(256 random bits)

### Authentication proof message (app → car)

JSON object containing:
- `type`: `"AUTH_PROOF"`
- `mac`: BytesToBase64(MAC(challenge, user key))

### Authentication confirmation message (car → app)

JSON object containing:
- `type`: `"AUTH_CONFIRMATION"`

### Authentication failure message (car → app)

JSON object containing:
- `type`: `"AUTH_FAILURE"`


## User configuration write

1. The app sends a **user configuration write request message** containing the configuration object described in the scenario
1. The car generates an IV
1. The car stores the configuration object encrypted with the user's key and the IV in its database, along with the IV
1. The car replies with a **user configuration write confirmation message**

### User configuration write request message

JSON object containing:
- `type`: `"USER_CONFIG_WRITE_REQUEST"`
- `configuration`: the JSON object described in the scenario

### User configuration write confirmation message

JSON object containing:
- `type`: `"USER_CONFIG_WRITE_CONFIRMATION"`


## User configuration read

1. The app sends a **user configuration read request message**.
1. The car retrieves and decrypts the user's configuration from its database and replies with a **user configuration read response message**.

### User configuration read request message

JSON object containing:
- `type`: `"USER_CONFIG_READ_REQUEST"`

### User configuration read response message

JSON object containing:
- `type`: `"USER_CONFIG_READ_RESPONSE"`
- `configuration`: the JSON object described in the scenario

## Shared info read

**TODO**

## Firmware update

**TODO**
