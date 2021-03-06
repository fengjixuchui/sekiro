package com.virjar.sekiro.netty.protocol;

import com.virjar.sekiro.log.SekiroLogger;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class SekiroNatMessageDecoder extends ByteToMessageDecoder {

    private static final byte HEADER_SIZE = 4;

    private static final int TYPE_SIZE = 1;

    private static final int SERIAL_NUMBER_SIZE = 8;

    private static final int URI_LENGTH_SIZE = 1;


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (true) {
            if (in.readableBytes() < HEADER_SIZE) {
                return;
            }

            int originIndex = in.readerIndex();

            int frameLength = in.readInt();
            if (in.readableBytes() < frameLength) {
                in.readerIndex(originIndex);
                return;
            }
            try {
                SekiroNatMessage sekiroNatMessage = new SekiroNatMessage();
                byte type = in.readByte();
                long sn = in.readLong();

                sekiroNatMessage.setSerialNumber(sn);

                sekiroNatMessage.setType(type);

                byte uriLength = in.readByte();
                byte[] uriBytes = new byte[uriLength];
                in.readBytes(uriBytes);
                sekiroNatMessage.setExtra(new String(uriBytes));

                int dataLength = frameLength - TYPE_SIZE - SERIAL_NUMBER_SIZE - URI_LENGTH_SIZE - uriLength;
                if (dataLength < 0) {
                    SekiroLogger.error("message protocol error,negative data length:" + dataLength + " for channel: " + ctx.channel()
                            + " frameLength: " + frameLength + " type:" + type + " serial_number:" + sn + " uriLength:" + uriLength + " extra:" + sekiroNatMessage.getExtra()
                    );
                    ctx.channel().close();
                    return;
                }

                if (dataLength > 0) {
                    byte[] data = new byte[dataLength];
                    in.readBytes(data);
//                    CompressUtil.Extra extra = CompressUtil.parseContextType(sekiroNatMessage.getExtra());
//                    if (CompressUtil.canCompress(extra)) {
//                        CompressUtil.CompressResponse uncompress = CompressUtil.uncompress(data);
//                        sekiroNatMessage.setData(uncompress.getSrc());
////                      // 替换实际的context type
//                        sekiroNatMessage.setExtra(extra.getContextType());
//                        SekiroLogger.info("use uncompress before length "
//                                + data.length + " after length " + uncompress.getSrc().length);
//                    } else {
                    sekiroNatMessage.setData(data);
                    // }
                }
                out.add(sekiroNatMessage);
                // in.release();
            } catch (Exception e) {
                SekiroLogger.error("message decode failed for channel: " + ctx.channel(), e);
                //协议都紊乱了，不知道啥原因。所以直接close
                ctx.channel().close();
            }

        }
    }
}