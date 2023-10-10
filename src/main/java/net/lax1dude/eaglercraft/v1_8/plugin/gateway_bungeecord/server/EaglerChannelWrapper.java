package net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.server;

import io.netty.channel.ChannelHandlerContext;
import net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.server.bungeeprotocol.EaglerBungeeProtocol;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.Protocol;

/**
 * Copyright (c) 2022-2023 LAX1DUDE. All Rights Reserved.
 * 
 * WITH THE EXCEPTION OF PATCH FILES, MINIFIED JAVASCRIPT, AND ALL FILES
 * NORMALLY FOUND IN AN UNMODIFIED MINECRAFT RESOURCE PACK, YOU ARE NOT ALLOWED
 * TO SHARE, DISTRIBUTE, OR REPURPOSE ANY FILE USED BY OR PRODUCED BY THE
 * SOFTWARE IN THIS REPOSITORY WITHOUT PRIOR PERMISSION FROM THE PROJECT AUTHOR.
 * 
 * NOT FOR COMMERCIAL OR MALICIOUS USE
 * 
 * (please read the 'LICENSE' file this repo's root directory for more info) 
 * 
 */
public class EaglerChannelWrapper extends ChannelWrapper {

	public EaglerChannelWrapper(ChannelHandlerContext ctx) {
		super(ctx);
	}

	public void setProtocol(EaglerBungeeProtocol protocol) {
		getHandle().pipeline().get(EaglerMinecraftEncoder.class).setProtocol(protocol);
		getHandle().pipeline().get(EaglerMinecraftDecoder.class).setProtocol(protocol);
	}

	public void setVersion(int protocol) {
		getHandle().pipeline().get(EaglerMinecraftEncoder.class).setProtocolVersion(protocol);
		getHandle().pipeline().get(EaglerMinecraftDecoder.class).setProtocolVersion(protocol);
	}

	public Protocol getEncodeProtocol() {
		EaglerBungeeProtocol eaglerProtocol = getHandle().pipeline().get(EaglerMinecraftEncoder.class).getProtocol();
		switch (eaglerProtocol) {
			case GAME:
				return Protocol.GAME;
			case HANDSHAKE:
				return Protocol.HANDSHAKE;
			case LOGIN:
				return Protocol.LOGIN;
			case STATUS:
				return Protocol.STATUS;
		}
		return null;
	}
	
	public void close(Object o) {
		super.close(o);
		EaglerPipeline.closeChannel(getHandle());
	}
	
}
