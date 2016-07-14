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

package com.splicemachine.customer;

import com.splicemachine.db.shared.common.udt.UDTBase;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Price extends UDTBase
{
  private static final int FIRST_VERSION = 0;
  public String currencyCode;
  public double amount;

  public Price()
  {
  }

  public Price(String paramString, double paramDouble)
  {
    this.currencyCode = paramString;
    this.amount = paramDouble;
  }

  public void writeExternal(ObjectOutput paramObjectOutput)
    throws IOException
  {
    super.writeExternal(paramObjectOutput);

    paramObjectOutput.writeBoolean(this.currencyCode != null);
    if (this.currencyCode != null) {
      paramObjectOutput.writeUTF(this.currencyCode);
    }
    paramObjectOutput.writeDouble(this.amount);
  }

  public void readExternal(ObjectInput paramObjectInput)
    throws IOException, ClassNotFoundException
  {
    super.readExternal(paramObjectInput);

    if (paramObjectInput.readBoolean()) {
      this.currencyCode = paramObjectInput.readUTF();
    }
    this.amount = paramObjectInput.readDouble();
  }
}