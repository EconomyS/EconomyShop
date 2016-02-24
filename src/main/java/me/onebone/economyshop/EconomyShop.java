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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyshop.provider.Provider;
import me.onebone.economyshop.provider.YamlProvider;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class EconomyShop extends PluginBase implements Listener{
	private Provider provider = null;
	private Map<String, Shop> shops;
	private Map<String, Object[]> queue;
	private Map<String, String> lang;
	private Map<Player, Long> taps;
	
	private Map<Level, List<ItemDisplayer>> displayers = null;
	
	private EconomyAPI api;
	
	public String getMessage(String key){
		return this.getMessage(key, new String[]{});
	}
	
	public String getMessage(String key, Object[] params){
		if(this.lang.containsKey(key)){
			return replaceMessage(this.lang.get(key), params);
		}
		return "Could not find message with " + key;
	}
	
	private String replaceMessage(String lang, Object[] params){
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < lang.length(); i++){
			char c = lang.charAt(i);
			if(c == '{'){
				int index;
				if((index = lang.indexOf('}', i)) != -1){
					try{
						String p = lang.substring(i + 1, index);
						if(p.equals("M")){
							i = index;
							
							builder.append(api.getMonetaryUnit());
							continue;
						}
						int param = Integer.parseInt(p);
						
						if(params.length > param){
							i = index;
							
							builder.append(params[param]);
							continue;
						}
					}catch(NumberFormatException e){}
				}
			}else if(c == '&'){
				char color = lang.charAt(++i);
				if((color >= '0' && color <= 'f') || color == 'r' || color == 'l' || color == 'o'){
					builder.append(TextFormat.ESCAPE);
					builder.append(color);
					continue;
				}
			}
			
			builder.append(c);
		}
		
		return builder.toString();
	}
	
	@SuppressWarnings("unchecked")
	public void onEnable(){
		this.saveDefaultConfig();
		
		InputStream is = this.getResource("lang_" + this.getConfig().get("langauge", "en") + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_en.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		api = EconomyAPI.getInstance();
		
		this.queue = new HashMap<>();
		this.displayers = new HashMap<>(); 
		this.taps = new HashMap<>();
		
		this.provider = new YamlProvider(this);
		
		shops = new HashMap<>();
		this.provider.getAll().forEach((k, v) -> {
			List<Object> val = (List<Object>)v;
			Shop shop = new Shop(this.getServer(), (int)val.get(0), (int)val.get(1), (int)val.get(2), (String)val.get(3),
					Item.get((int)val.get(4), (int)val.get(5), (int)val.get(7)),
					(double)val.get(8),
					(int)val.get(9));
			
			Position pos = shop.getPosition();
			shops.put(pos.x + ":" + pos.y + ":" + pos.z + ":" + pos.level.getFolderName(), shop);
		});
		
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("shop")){
			if(args.length < 1){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			
			if(args[0].equals("create")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				if(args.length < 4){
					sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
					return true;
				}
				
				try{
					int amount = Integer.parseInt(args[2]);
					float price = Float.parseFloat(args[3]);
					
					queue.put(sender.getName().toLowerCase(), new Object[]{
						true, Item.fromString(args[1]), amount, price
					});
					
					sender.sendMessage(this.getMessage("added-queue"));
				}catch(NumberFormatException e){
					sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				}
				return true;
			}else if(args[0].equals("remove")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				queue.put(sender.getName().toLowerCase(), new Object[]{
					false
				});
				sender.sendMessage(this.getMessage("added-rm-queue"));
			}else{
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
			}
		}
		return false;
	}
	
	@EventHandler
	public void onTouch(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Position pos = (Position)event.getBlock();
		
		String key = pos.x + ":" + pos.y + ":" + pos.z + ":" + pos.level.getFolderName();
		
		if(queue.containsKey(player.getName().toLowerCase())){
			Object[] info = queue.get(player.getName().toLowerCase());
			
			if((boolean) info[0]){
				if(!this.shops.containsKey(key)){
					this.provider.addShop(pos, (Item)info[1], (float) info[3], (int) info[2]);
					
					Shop shop = new Shop(pos, (Item)info[1], (float) info[3], (int) info[2]);
					
					this.shops.put(key, shop);
					if(!this.displayers.containsKey(pos.level)){
						this.displayers.put(pos.level, new ArrayList<ItemDisplayer>());
					}
					this.displayers.get(pos.level).add(shop.getDisplayer());
					
					queue.remove(player.getName().toLowerCase());
					
					player.sendMessage(this.getMessage("shop-created"));
				}else{
					player.sendMessage(this.getMessage("shop-already-exist"));
				}
			}else{
				if(this.shops.containsKey(key)){
					this.provider.removeShop(pos);
					
					Shop shop = this.shops.get(key);
					
					if(this.displayers.containsKey(pos.level)){
						this.displayers.get(pos.level).remove(shop.getDisplayer());
					}
					this.shops.remove(key);
					
					queue.remove(player.getName().toLowerCase());
					
					player.sendMessage(this.getMessage("shop-removed"));
				}
			}
		}else{
			Shop shop = this.shops.get(key);
			
			if(shop != null){
				Item item = shop.getItem();
				
				if(this.getConfig().get("purchase.tap-twice", true)){
					long now = System.currentTimeMillis();
					if(!this.taps.containsKey(player) || now - this.taps.get(player) > 1000){
						player.sendMessage(this.getMessage("tap-again", new Object[]{
								item.getName(), item.getCount(), shop.getPrice()
						}));
						
						this.taps.put(player, now);
						return;
					}else{
						this.taps.remove(player);
					}
				}
				
				if(player.hasPermission("economyshop.purchase")){
					if(this.api.myMoney(player) >= shop.getPrice()){
						if(!player.getInventory().canAddItem(item)){
							player.sendMessage(this.getMessage("full-inventory"));
							return;
						}
						
						this.api.reduceMoney(player, shop.getPrice(), true);
						player.getInventory().addItem(item);
						player.sendMessage(this.getMessage("bought-item", new Object[]{
								item.getName(), item.getCount(), shop.getPrice()
						}));
					}else{
						player.sendMessage(this.getMessage("no-money", new Object[]{
								shop.getPrice(), item.getName() 
						}));
					}
				}else{
					player.sendMessage(this.getMessage("no-permission-buy"));
				}
			}
		}
	}
	
	public void onDisable(){
		if(provider != null){
			this.provider.close();
		}
	}
}
