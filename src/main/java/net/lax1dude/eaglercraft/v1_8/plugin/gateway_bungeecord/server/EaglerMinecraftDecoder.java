package net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.EaglerXBungee;
import net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.server.bungeeprotocol.EaglerBungeeProtocol;
import net.lax1dude.eaglercraft.v1_8.plugin.gateway_bungeecord.server.bungeeprotocol.EaglerProtocolAccessProxy;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants.Direction;

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
public class EaglerMinecraftDecoder extends MessageToMessageDecoder<WebSocketFrame> {
	private EaglerBungeeProtocol protocol;
	private final boolean server;
	private int protocolVersion;

	@Override
	protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
		if(!ctx.channel().isActive()) {
			return;
		}
		EaglerConnectionInstance con = ctx.channel().attr(EaglerPipeline.CONNECTION_INSTANCE).get();
		long millis = System.currentTimeMillis();
		if(frame instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame in = (BinaryWebSocketFrame) frame;
			ByteBuf buf = in.content();
			int pktId = DefinedPacket.readVarInt(buf);
			DefinedPacket pkt = EaglerProtocolAccessProxy.createPacket(protocol, protocolVersion, pktId, server);
			Protocol bungeeProtocol = null;
			switch (this.protocol) {
				case GAME:
					bungeeProtocol = Protocol.GAME;
					break;
				case HANDSHAKE:
					bungeeProtocol = Protocol.HANDSHAKE;
					break;
				case LOGIN:
					bungeeProtocol = Protocol.LOGIN;
					break;
				case STATUS:
					bungeeProtocol = Protocol.STATUS;
					break;
			}
			if(pkt != null) {
				pkt.read(buf, server ? Direction.TO_CLIENT : Direction.TO_SERVER, protocolVersion);
				if(buf.isReadable()) {
					EaglerXBungee.logger().severe("[DECODER][" + ctx.channel().remoteAddress() + "] Packet "  +
							pkt.getClass().getSimpleName() + " had extra bytes! (" + buf.readableBytes() + ")");
				}else {
					out.add(wrapPacket(pkt, buf, bungeeProtocol));
				}
			}else {
				out.add(wrapPacket(null, buf, bungeeProtocol));
			}
		}else if(frame instanceof PingWebSocketFrame) {
			if(millis - con.lastClientPingPacket > 500l) {
				ctx.write(new PongWebSocketFrame());
				con.lastClientPingPacket = millis;
			}
		}else if(frame instanceof PongWebSocketFrame) {
			con.lastClientPongPacket = millis;
		}else {
			ctx.close();
		}
	}

	public EaglerMinecraftDecoder(final EaglerBungeeProtocol protocol, final boolean server, final int protocolVersion) {
		this.protocol = protocol;
		this.server = server;
		this.protocolVersion = protocolVersion;
	}

	public void setProtocol(final EaglerBungeeProtocol protocol) {
		this.protocol = protocol;
	}

	public void setProtocolVersion(final int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	private static Constructor<PacketWrapper> packetWrapperConstructor = null;

	private PacketWrapper wrapPacket(DefinedPacket packet, ByteBuf buf, Protocol protocol) {
		ByteBuf cbuf = null;
		try {
			cbuf = buf.copy(0, buf.writerIndex());
			if (packetWrapperConstructor == null) {
				try {
					PacketWrapper pkt = new PacketWrapper(packet, cbuf, protocol);
					cbuf = null;
					return pkt;
				} catch (NoSuchMethodError e) {
					try {
                        packetWrapperConstructor = PacketWrapper.class.getDeclaredConstructor(DefinedPacket.class, ByteBuf.class);
                        PacketWrapper pkt = packetWrapperConstructor.newInstance(packet, cbuf);
						cbuf = null;
						return pkt;
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
							 NoSuchMethodException ex) {
						throw new RuntimeException(ex);
					}
				}
			} else {
				try {
					PacketWrapper pkt = packetWrapperConstructor.newInstance(packet, cbuf);
					cbuf = null;
					return pkt;
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}finally {
			if(cbuf != null) {
				cbuf.release();
			}
		}
	}
}
