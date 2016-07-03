# EconomyShop
A plugin which allows your server to create shops

## Build
- git clone https://github.com/onebone/EconomyShop && cd EconomyShop
- mvn clean
- mvn package

## Commands
/shop `<create|remove> [item[:damage]] [amount] [price] [side]`

/buy `[amount]`

## Permissions
- economyshop
  - economyshop.command
    - economyshop.command.shop
	  - economyshop.command.shop.create
	  - economyshop.command.shop.remove
	- economyshop.command.buy
  - economyshop.purchase
