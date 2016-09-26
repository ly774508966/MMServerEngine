package com.mm.engine.sysBean.entrance;

import com.mm.engine.framework.control.request.RequestService;
import com.mm.engine.framework.data.entity.account.AccountSysService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.net.code.HttpDecoder;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.entrance.Entrance;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by a on 2016/9/20.
 */
public class RequestJettyPBEntrance extends Entrance {

    private static final Logger log = LoggerFactory.getLogger(RequestJettyPBEntrance.class);

    private Server server;


    public RequestJettyPBEntrance(){}

    private SessionService sessionService;
    private RequestService requestService;
    private AccountSysService accountSysService;
    @Override
    public void start() throws Exception {
//        System.out.println(sessionService);
        Handler entranceHandler = new AbstractHandler(){
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
                fire(request,response,"RequestJettyPBEntrance");
            }
        };

        server = new Server(this.port);
        server.setHandler(entranceHandler);
        server.start();
    }

    private void fire(HttpServletRequest request, HttpServletResponse response,String entranceName){
        try {
            byte[] data = HttpDecoder.decode(request);
            // 获取controller，并根据controller获取相应的编解码器
            String opcodeStr = request.getHeader(SysConstantDefine.opcodeKey);
            if(StringUtils.isEmpty(opcodeStr) || !StringUtils.isNumeric(opcodeStr)){
                throw new MMException("opcode error");
            }
            int opcode=Integer.parseInt(opcodeStr);
            
            Session session = sessionService.create(request.getRequestURL().toString(), Util.getIp(request));
            RetPacket rePacket = requestService.handle(opcode, data, session);
            if(rePacket==null){
                // 处理包失败
                throw new MMException("处理消息错误,opcode:"+opcode);
            }
            if(!rePacket.keepSession()){
                sessionService.removeSession(session);
            }
            response.setHeader(SysConstantDefine.opcodeKey,""+rePacket.getOpcode());

            byte[] reData=(byte[])rePacket.getRetData();
            // 这个地方要+1
            response.setBufferSize(reData.length+1);
            response.setContentLength(reData.length);
            response.getOutputStream().write(reData, 0, reData.length);
            response.getOutputStream().flush();
//            response.getOutputStream().close();
        }catch (Throwable e){
            // TODO 两种更可能：MMException和非MMException
            e.printStackTrace();
            throw new RuntimeException(entranceName+" Exception");
        }
    }

    @Override
    public void stop() throws Exception {
        if(server != null){
            server.stop();
        }
    }
}