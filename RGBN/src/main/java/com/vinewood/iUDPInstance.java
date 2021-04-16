package com.vinewood;



public interface iUDPInstance {
    public void SendPacket();
    /**
     * 
     * @return Data segment
     */
    public byte[] ReceivePacket();

}
