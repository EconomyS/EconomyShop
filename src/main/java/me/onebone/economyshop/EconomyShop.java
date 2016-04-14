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
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.entity.EntityTeleportEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
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
			}
			
			builder.append(c);
		}
		
		return TextFormat.colorize(builder.toString());
	}
	
	@SuppressWarnings("unchecked")
	public void onEnable(){
		this.saveDefaultConfig();
		
		String name = this.getConfig().get("language", "en");
		InputStream is = this.getResource("lang_" + name + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_en.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		if(!name.equals("en")){
			try{
				LinkedHashMap<String, String> temp = new GsonBuilder().create().fromJson(Utils.readFile(this.getResource("lang_en.json")), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
				temp.forEach((k, v) -> {
					if(!lang.containsKey(k)){
						lang.put(k, v);
					}
				});
			}catch(IOException e){
				this.getLogger().critical(e.getMessage());
			}
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
			
			if(shop.getDisplayer() != null){ // shop item display option is 'none'
				if(!displayers.containsKey(shop.getPosition().getLevel())){
					displayers.put(shop.getPosition().getLevel(), new ArrayList<ItemDisplayer>());
				}
				
				displayers.get(shop.getPosition().getLevel()).add(shop.getDisplayer());
			}
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
				
				int side = -1;
				if(args.length > 4){
					try{
						side = Integer.parseInt(args[4]);
					}catch(NumberFormatException e){
						switch(args[4].toLowerCase()){
						case "down": side = Vector3.SIDE_DOWN; break;
						case "east": side = Vector3.SIDE_EAST; break;
						case "north": side = Vector3.SIDE_NORTH; break;
						case "south": side = Vector3.SIDE_SOUTH; break;
						case "up": side = Vector3.SIDE_UP; break;
						case "west": side = Vector3.SIDE_WEST; break;
						case "shop": side = -1; break;
						case "none": side = -2; break;
						default:
							sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
							return true;
						}
					}
				}
				
				if(side < -2 && side > 5){
					sender.sendMessage(this.getMessage("invalid-side"));
					return true;
				}
				
				try{
					int amount = Integer.parseInt(args[2]);
					float price = Float.parseFloat(args[3]);
					
					Item item = Item.fromString(args[1]);
					item.setCount(amount);
					queue.put(sender.getName().toLowerCase(), new Object[]{
						true, item, side, price
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
					
					if(shop.getDisplayer() != null){
						if(!this.displayers.containsKey(pos.level)){
							this.displayers.put(pos.level, new ArrayList<ItemDisplayer>());
						}
						this.displayers.get(pos.level).add(shop.getDisplayer());
					
						shop.getDisplayer().spawnToAll(shop.getPosition().getLevel());
					}
					
					queue.remove(player.getName().toLowerCase());
					
					player.sendMessage(this.getMessage("shop-created"));
				}else{
					player.sendMessage(this.getMessage("shop-already-exist"));
				}
			}else{
				if(this.shops.containsKey(key)){
					this.provider.removeShop(pos);
					
					Shop shop = this.shops.get(key);
					
					if(shop.getDisplayer() != null){
						if(this.displayers.containsKey(pos.level)){
							this.displayers.get(pos.level).remove(shop.getDisplayer());
						}
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
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		
		if(this.displayers.containsKey(player.getLevel())){
			this.displayers.get(player.getLevel()).forEach(displayer -> displayer.spawnTo(player));
		}
	}
	
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Position pos = event.getBlock();
        String key = pos.x + ":" + pos.y + ":" + pos.z + ":" + pos.level.getFolderName();

        if (this.shops.containsKey(key)) {
            if (!event.getPlayer().hasPermission("economyshop.break")) {
                event.getPlayer().sendMessage(this.getMessage("shop-breaking-forbidden"));
                event.setCancelled();
                return;
            }
            this.provider.removeShop(pos);
            this.shops.remove(key);
            event.getPlayer().sendMessage(this.getMessage("shop-removed"));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();

        if (lines[0].equalsIgnoreCase("shop") || lines[0].equalsIgnoreCase("[shop]")) {
			Position pos = event.getBlock();
			String key = pos.x + ":" + pos.y + ":" + pos.z + ":" + pos.level.getFolderName();
			if(!this.shops.containsKey(key)){
				Player player = event.getPlayer();
				
				if(player.hasPermission("economyshop.create")){
					float price;
					int amount;
					
					try{
						price = Float.parseFloat(lines[1]);
						amount = Integer.parseInt(lines[3]);
					}catch(NumberFormatException e){
						player.sendMessage(this.getMessage("invalid-format"));
						return;
					}
					
					Item item = Item.fromString(lines[2]);
					item.setCount(amount);
					
					this.provider.addShop(pos, item, price, -2);
					
					Shop shop = new Shop(pos, item, price, -2);
					
					this.shops.put(key, shop);
					
					event.setLine(0, this.getMessage("sign-text-1"));
					event.setLine(1, this.getMessage("sign-text-2", new Object[]{price}));
					event.setLine(2, this.getMessage("sign-text-3", new Object[]{item.getName()}));
					event.setLine(3, this.getMessage("sign-text-4", new Object[]{amount}));
					
					player.sendMessage(this.getMessage("shop-created"));
				}
			}
		}
	}
	
	@EventHandler
	public void onTeleport(EntityTeleportEvent event){
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			
			Position from = event.getFrom();
			Position to = event.getTo();
			
			if(from.getLevel() != to.getLevel()){
				if(this.displayers.containsKey(from.getLevel())){
					this.displayers.get(from.getLevel()).forEach((displayer) -> displayer.despawnFrom(player));
				}
				
				if(this.displayers.containsKey(to.getLevel())){
					this.displayers.get(to.getLevel()).forEach((displayer) -> displayer.spawnTo(player));
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
