package biblioteca;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/** Abre y cierra programáticamente el túnel SSH hacia la EC2 para llegar al RDS privado. */
public class SshTunnel implements AutoCloseable {
    private Session session;

    public void open(Config c) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(c.get("ssh.keyfile"));

        session = jsch.getSession(c.get("ssh.user"), c.get("ssh.host"), c.getInt("ssh.port"));
        // Entorno académico: no se verifica known_hosts. En producción debe verificarse.
        session.setConfig("StrictHostKeyChecking", "no");
        session.setServerAliveInterval(30000);
        session.connect(10000);

        // localhost:local.port  ->  (a través de la EC2)  ->  rds.host:rds.port
        session.setPortForwardingL(c.getInt("local.port"), c.get("rds.host"), c.getInt("rds.port"));
        System.out.println("[SSH] Túnel abierto: 127.0.0.1:" + c.getInt("local.port")
                + " -> " + c.get("rds.host") + ":" + c.getInt("rds.port"));
    }

    public boolean isOpen() {
        return session != null && session.isConnected();
    }

    @Override
    public void close() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("[SSH] Sesión SSH cerrada.");
        }
    }
}
