# PayBySign Plugin for Minecraft

**PayBySign** is a Minecraft plugin that allows players to pay for redstone services using signs. This plugin integrates an economic system based on paying for specific actions or services triggered by interacting with signs.

## Features

- **Economics with Redstone**: Allows players to pay for redstone services using payment signs.
- **Customizable Payments**: Define specific amounts of money and set conditions for payments.
- **Player-to-Player Payment**: Players can create payment signs for others with appropriate permissions.

## Planned Updates

- **Future Updates**: We are planning to add more customization options for payment signs and integrate with other economy plugins.

## Dependencies

- **Vault**: Required for economy integration. Ensure Vault is installed on your server.

## Installation

1. Download the `PayBySign.jar` file.
2. Place the `.jar` file into the `plugins` folder of your server.
3. Install and configure the **Vault** plugin for economy support.
4. Restart your server.
5. Customize settings through the `config.yml` and `messages.yml` files.

## Permissions

### paybysign.*
- **Description**: Grants access to all features of the plugin.

### paybysign.create
- **Description**: Grants permission to create payment signs.

### paybysign.create.other
- **Description**: Grants permission to create payment signs for other players.

### paybysign.use
- **Description**: Grants permission to use payment signs.

## Creating a Payment Sign

To create a payment sign, follow these steps:

1. Ensure you have the `paybysign.create` permission.
2. Place a sign with the following format:

- **[PayBySign]**: This is the identifier for the payment sign.
- **{Player Name}**: The name of the player to be paid.
- **{Money Amount}**: The amount of money the player must pay.
- **{Time Limit}**: The time limit in seconds, if applicable, for the payment.

---

Enjoy using PayBySign for your redstone economy!
