package tprov.model;

public class TomcatInstance {
	String containerId;
	String hostname;
	String ip;
	String port;
	
	public String getContainerId() {
		return containerId;
	}
	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	@Override
	public String toString() {
		return "TomcatInstance [containerId=" + containerId + ", hostname="
				+ hostname + ", ip=" + ip + ", port=" + port + "]";
	}
}
