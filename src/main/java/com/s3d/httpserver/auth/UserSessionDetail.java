package com.s3d.httpserver.auth;

import java.io.Serializable;

/**
 * 用户会话信息
 * @author sulta
 *
 */
public interface UserSessionDetail extends Serializable{
	
	public String getSessionId();
	
	public String getUserId();
}
