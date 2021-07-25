package com.swz.rpc.transport.netty.protocol;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.config.RpcConfig;
import com.swz.rpc.config.SerializationTypeEnum;
import com.swz.rpc.serializer.Serializer;
import com.swz.rpc.pojo.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.Arrays;
import java.util.List;

/**
 * 消息编解码器
 * -------------------------------------------------------------------
 * magic  | version | codec | messageType |padding|length |
 *(4byte) | (1byte) |(1byte)|   (1byte)   |(1byte)|(4byte)|
 * -------------------------------------------------------------------
 *                       content
 *                  ($(length) byte)
 *--------------------------------------------------------------------
 * @author 向前走不回头
 * @date 2021/7/23
 */
@ChannelHandler.Sharable
public class MessageCodec extends MessageToMessageCodec<ByteBuf, Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        final ByteBuf buffer = ctx.alloc().buffer();
//          4字节的魔数
        buffer.writeBytes(RpcConfig.MAGIC_NUMBER);
//        1个字节的版本号
        buffer.writeByte(RpcConfig.VERSION);
//        1个字节的序列化方法
        buffer.writeByte(msg.getCodec());
//        1个字节的消息类型
        buffer.writeByte(msg.getMessageType());
//        1个字节的对齐填充
        buffer.writeByte(0xff);
        Serializer serializer = SerializationTypeEnum.getSerializer(SerializationTypeEnum.getName(msg.getCodec()));
        byte[] content = serializer.serialize(msg);
        int length = content.length;
//        4个字节的内容长度
        buffer.writeInt(length);
//        length个字节的内容
        buffer.writeBytes(content);
        out.add(buffer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        checkMagicNumber(byteBuf);
        checkVersion(byteBuf);
//      1个字节的序列化类型
        byte codecType = byteBuf.readByte();
        //        1个字节的消息类型
        byte messageType = byteBuf.readByte();
        Serializer serializer = SerializationTypeEnum.getSerializer(SerializationTypeEnum.getName(codecType));
        //        1个字节的对齐填充
        byteBuf.readByte();
        // 4个字节的长度
        int length = byteBuf.readInt();
        Message.getMessageClass(messageType);

        byte[] temp = new byte[length];
        byteBuf.readBytes(temp,0,length);
        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message message = serializer.deserialize(messageClass, temp);
        out.add(message);
    }

    public void checkMagicNumber(ByteBuf in){
        final byte[] bytes = new byte[RpcConfig.MAGIC_NUMBER.length];
        in.readBytes(bytes);
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != RpcConfig.MAGIC_NUMBER[i]){
                throw new RpcException("魔数不匹配，未知的魔数 " + Arrays.toString(bytes));
            }
        }
    }
    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (RpcConfig.VERSION != version){
            throw new RpcException("版本不匹配 期待版本为" + RpcConfig.VERSION );
        }
    }
}
