package me.onebone.economyshop;

/*
 * EconomyShop: A plugin which allows your server to create shops
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;

public class Shop{
	private Position pos;
	private Item item;
	private double price;
	private ItemDisplayer displayer;
	
	public Shop(Position pos, Item item, double price, int side){
		this.pos = pos;
		this.item = item;
		this.price = price;
		
		if(side == -1){
			this.displayer = new ItemDisplayer(this, new Position(pos.x, pos.y, pos.z, pos.level), item);
		}else if(side == -2){
			return;
		}else{
			this.displayer = new ItemDisplayer(this, pos.getSide(side), item);
		}
	}
	
	public Shop(int x, int y, int z, Level level, Item item, double price, int side){
		this(new Position(x, y, z, level), item, price, side);
	}
	
	public Shop(Server server, int x, int y, int z, String level, Item item, double price, int side){
		this(new Position(x, y, z, server.getLevelByName(level)), item, price, side);
	}
	
	public Position getPosition(){
		return this.pos;
	}
	
	public ItemDisplayer getDisplayer(){
		return this.displayer;
	}
	
	public Item getItem(){
		return Item.get(this.item.getId(), this.item.getDamage(), this.item.getCount());
	}
	
	public double getPrice(){
		return this.price;
	}
}
