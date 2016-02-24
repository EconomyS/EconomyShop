package me.onebone.economyshop.provider;

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

import java.util.List;
import java.util.Map;

import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;

public interface Provider{
	public void addShop(Position pos, Item item, float price, int side);
	public void addShop(int x, int y, int z, Level level, Item item, float price, int side);
	public void addShop(int x, int y, int z, String level, Item item, float pice, int side);
	
	public void removeShop(Position pos);
	public void removeShop(int x, int y, int z, Level level);
	public void removeShop(int x, int y, int z, String level);
	
	public List<Object> getShop(Position pos);
	public List<Object> getShop(int x, int y, int z, Level level);
	public List<Object> getShop(int x, int y, int z, String level);
	
	public Map<String, Object> getAll();
	
	public void save();
	public void close();
}
