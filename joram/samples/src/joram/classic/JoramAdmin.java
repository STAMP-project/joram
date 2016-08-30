/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2001 - 2013 ScalAgent Distributed Technologies
 * Copyright (C) 2004 - Bull SA
 * Copyright (C) 1996 - 2000 Dyade
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA.
 *
 * Initial developer(s): ScalAgent Distributed Technologies
 * Contributor(s): 
 */
package classic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class JoramAdmin {
  final static String PROMPT = "-> ";
  
  public static void main(String[] args) throws IOException {
    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;   
    
    try {
      if (args.length == 3) {
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String command = args[2];
        
        try {
          socket = new Socket(hostname, port);
          out = new PrintWriter(socket.getOutputStream(), true);
          in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
          return;
        }
                
        out.println(command);
        out.println("\004");
        
        String line;
        do {
          line = in.readLine();
          if ((line != null) && (! line.startsWith(PROMPT)))
            System.out.println(line);
        } while (line != null);
        
      } else {
        System.out.println("usage: java ... classic.JoramAdmin <host> <port> <command>");
      }
    } catch (Exception exc) {
      System.out.println("usage: java ... classic.JoramAdmin <host> <port> <command>");
      exc.printStackTrace();
    } finally {
      if (out != null) out.close();
      if (in != null) in.close();
      if (socket != null) socket.close();
    }
  }

}
