// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// SFTP -- Methods for using SFTP.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// --------------------------------------------------------------------------

package org.metastatic.net.ssh2;

public class SFTP extends Thread implements ChannelListener, SFTPConstants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   Channel c;

   // Constructors.
   // -----------------------------------------------------------------------

   public SFTP() {

   }

   // Methods implementing ChannelListener. ---------------------------------

   public void startInput(Channel c) {
      this.c = c;
      c.writeData
   }

   static class PacketInput {
      
   }

   static class PacketOutput
}
