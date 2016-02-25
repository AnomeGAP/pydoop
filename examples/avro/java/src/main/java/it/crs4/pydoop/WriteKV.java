/** BEGIN_COPYRIGHT
 *
 * Copyright 2009-2016 CRS4.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * END_COPYRIGHT
 *
 * Read user data generated by create_input.py and create a key/value
 * avro file with those users as keys.
 */

package it.crs4.pydoop;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.hadoop.io.AvroKeyValue;


class WriteKV {

  private static final String DELIMITER = ";";

  private static GenericRecord buildUser(
      Schema schema, String name, String office, String color) {
    GenericRecord user = new GenericData.Record(schema);
    user.put("name", name);
    user.put("office", office);
    if (color != null) user.put("favorite_color", color);
    return user;
  }

  private static GenericRecord buildPet(
      Schema schema, String name, Integer legs) {
    GenericRecord pet = new GenericData.Record(schema);
    pet.put("name", name);
    pet.put("legs", legs);
    return pet;
  }

  private static <T> File createFile(File file, Schema schema, T... records)
      throws IOException {
    DatumWriter<T> datumWriter = new GenericDatumWriter<T>(schema);
    DataFileWriter<T> fileWriter = new DataFileWriter<T>(datumWriter);
    fileWriter.create(schema, file);
    for (T record: records) {
      fileWriter.append(record);
    }
    fileWriter.close();
    return file;
  }

  private static File createInputFile(
      Schema keySchema, Schema valueSchema, String inFN, String outFN
  ) throws IOException {
    Schema keyValueSchema = AvroKeyValue.getSchema(keySchema, valueSchema);
    List<GenericRecord> records = new ArrayList<GenericRecord>();
    BufferedReader reader = new BufferedReader(new FileReader(inFN));
    String line;
    int i = 0;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(DELIMITER);
      if (tokens.length != 3) {  // name, office, color
        throw new RuntimeException("Bad input format");
      }
      GenericRecord user = buildUser(
          keySchema, tokens[0], tokens[1], tokens[2]
      );
      GenericRecord pet = buildPet(valueSchema, String.format("pet-%d", i), i);
      AvroKeyValue<GenericRecord, GenericRecord> kv
          = new AvroKeyValue<GenericRecord, GenericRecord>(
              new GenericData.Record(keyValueSchema));
      kv.setKey(user);
      kv.setValue(pet);
      records.add(kv.get());
      i++;
    }
    reader.close();
    return createFile(
        new File(outFN), keyValueSchema,
        records.toArray(new GenericRecord[records.size()])
    );
  }

  public static void main(String[] args) throws Exception {

    if (args.length < 4) {
      System.err.println(
          "Usage: WriteKV USER_SCHEMA PET_SCHEMA IN_FILE OUT_FILE"
      );
      System.exit(1);
    }
    Schema.Parser parser = new Schema.Parser();
    Schema userSchema = parser.parse(new File(args[0]));
    Schema petSchema = parser.parse(new File(args[1]));

    File file = createInputFile(userSchema, petSchema, args[2], args[3]);
    System.out.println("wrote " + file.getName());

  }
}
