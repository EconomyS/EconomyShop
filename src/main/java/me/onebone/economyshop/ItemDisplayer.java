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

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;

public class ItemDisplayer{
	private long eid;
	private Shop link;
	private Position pos;
	private Item item;
	
	public ItemDisplayer(Shop link, Position pos, Item item){
		this.eid = Entity.entityCount++;
		
		this.link = link;
		this.pos = pos;
		this.item = item;
	}
	
	public Item getItem(){
		return this.item;
	}
	
	public Position getPosition(){
		return this.pos;
	}
	
	public Shop getLinked(){
		return this.link;
	}
	
	public void spawnTo(Player player){
		AddItemEntityPacket pk = new AddItemEntityPacket();
		pk.eid = this.eid;
		pk.item = item;
		pk.speedX = pk.speedY = pk.speedZ = 0;
		pk.x = (float) pos.x + 0.5F;
		pk.y = (float) pos.y;
		pk.z = (float) pos.z + 0.5F;
		
		player.dataPacket(pk);
	}
	
	public void spawnToAll(Level level){
		level.getPlayers().values().forEach(this::spawnTo);
	}
	
	public void despawnFrom(Player player){
		RemoveEntityPacket pk  = new RemoveEntityPacket();
		pk.eid = this.eid;
		
		player.dataPacket(pk);
	}
	
	public void despawnFromAll(Level level){
		level.getPlayers().values().forEach(this::despawnFrom);
	}
}
