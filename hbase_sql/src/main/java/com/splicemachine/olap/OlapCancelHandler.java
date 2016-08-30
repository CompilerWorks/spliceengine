/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.splicemachine.olap;

import com.splicemachine.olap.OlapMessage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

/**
 * @author Scott Fines
 *         Date: 4/1/16
 */
public class OlapCancelHandler extends AbstractOlapHandler{


    public OlapCancelHandler(OlapJobRegistry registry){
        super(registry);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception{
        OlapMessage.Command cmd=(OlapMessage.Command)e.getMessage();
        if(cmd.getType()!=OlapMessage.Command.Type.CANCEL){
            ctx.sendUpstream(e);
            return;
        }

        jobRegistry.clear(cmd.getUniqueName());
        //no response is needed for cancellation
    }
}
