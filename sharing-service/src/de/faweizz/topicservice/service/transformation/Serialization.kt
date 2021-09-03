package de.faweizz.topicservice.service.transformation

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import java.io.ByteArrayOutputStream

fun GenericRecord.serialize(): ByteArray {
    val writer = GenericDatumWriter<GenericRecord>(schema)
    val stream = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(stream, null)
    writer.write(this, encoder)
    encoder.flush()
    return stream.toByteArray()
}

fun ByteArray.deserialize(schema: Schema): GenericRecord {
    val reader = GenericDatumReader<GenericRecord>(schema)
    val decoder = DecoderFactory.get().binaryDecoder(this, null)
    return reader.read(null, decoder)
}