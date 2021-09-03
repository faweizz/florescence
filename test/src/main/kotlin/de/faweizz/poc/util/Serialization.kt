package de.faweizz.poc.util

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import java.io.ByteArrayOutputStream

fun avroDeserialize(value: ByteArray, schema: Schema): GenericRecord {
    val reader = GenericDatumReader<GenericRecord>(schema)
    val decoder = DecoderFactory.get().binaryDecoder(value, null)
    return reader.read(null, decoder)
}

fun avroSerialize(genericRecord: GenericRecord, schema: Schema): ByteArray {
    val writer = GenericDatumWriter<GenericRecord>(schema)
    val stream = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(stream, null)
    writer.write(genericRecord, encoder)
    encoder.flush()
    return stream.toByteArray()
}

fun GenericRecord.serialize(schema: Schema): ByteArray {
    return avroSerialize(this, schema)
}

fun ByteArray.deserialize(schema: Schema): GenericRecord {
    return avroDeserialize(this, schema)
}