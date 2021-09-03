package de.faweizz.topicservice.persistence

import de.faweizz.topicservice.service.client.Client
import de.faweizz.topicservice.service.joined.execution.CodeExecutionPoll
import de.faweizz.topicservice.service.joined.execution.CodeExecutionRequest
import de.faweizz.topicservice.service.joined.resource.JoinedResource
import de.faweizz.topicservice.service.joined.resource.JoinedResourceJoinRequest
import de.faweizz.topicservice.service.model.ResourceSharingRequest
import de.faweizz.topicservice.service.resource.Resource
import de.faweizz.topicservice.service.sharing.Transaction
import de.faweizz.topicservice.service.transformation.Transformation
import org.litote.kmongo.*

class Mongo {
    private val client = KMongo.createClient("mongodb://mongo")
    private val database = client.getDatabase("poc")

    fun saveTopic(resource: Resource) {
        database.getCollection<Resource>().save(resource)
    }

    fun findResourceById(name: String): Resource? {
        return database.getCollection<Resource>().findOne(Resource::id eq name)
    }

    fun saveResourceSharingRequest(resourceSharingRequest: ResourceSharingRequest) {
        return database.getCollection<ResourceSharingRequest>().save(resourceSharingRequest)
    }

    fun findResourceSharingRequestById(id: String): ResourceSharingRequest? {
        return database.getCollection<ResourceSharingRequest>().findOne(ResourceSharingRequest::id eq id)
    }

    fun updateTopicSharingRequest(request: ResourceSharingRequest) {
        database.getCollection<ResourceSharingRequest>().updateOne(ResourceSharingRequest::id eq request.id, request)
    }

    fun saveTransformation(transformation: Transformation) {
        return database.getCollection<Transformation>().save(transformation)
    }

    fun getAllTransformations(): List<Transformation> {
        return database.getCollection<Transformation>().find().toList()
    }

    fun findTransactionConcerning(actorId: String, resourceId: String): Transaction? {
        return database.getCollection<Transaction>()
            .findOne(Transaction::resourceId eq resourceId, Transaction::sharedWithActorId eq actorId)
    }

    fun saveClient(client: Client) {
        database.getCollection<Client>().save(client)
    }

    fun saveTransaction(transaction: Transaction) {
        database.getCollection<Transaction>().save(transaction)
    }

    fun getAllTransactions(): List<Transaction> {
        return database.getCollection<Transaction>().find().toList()
    }

    fun saveJoinedResourceJoinRequest(request: JoinedResourceJoinRequest) {
        database.getCollection<JoinedResourceJoinRequest>().save(request)
    }

    fun updateJoinedResourceJoinRequest(request: JoinedResourceJoinRequest) {
        database.getCollection<JoinedResourceJoinRequest>()
            .updateOne(JoinedResourceJoinRequest::id eq request.id, request)
    }

    fun findJoinedResourceJoinRequest(id: String): JoinedResourceJoinRequest? {
        return database.getCollection<JoinedResourceJoinRequest>()
            .find(JoinedResourceJoinRequest::id eq id)
            .firstOrNull()
    }

    fun findClient(name: String): Client? {
        return database.getCollection<Client>()
            .find(Client::name eq name)
            .firstOrNull()
    }

    fun findJoinedResourceByName(id: String): JoinedResource? {
        return database.getCollection<JoinedResource>().findOne(JoinedResource::resourceId eq id)
    }

    fun saveJoinedResource(joinedResource: JoinedResource) {
        database.getCollection<JoinedResource>().save(joinedResource)
    }

    fun updateSharedResource(joinedResource: JoinedResource) {
        database.getCollection<JoinedResource>()
            .updateOne(JoinedResource::resourceId eq joinedResource.resourceId, joinedResource)
    }

    fun findCodeExecutionPollById(pollableId: String): CodeExecutionPoll? {
        return database.getCollection<CodeExecutionPoll>().findOne(CodeExecutionPoll::id eq pollableId)
    }

    fun updateCodeExecutionPollById(pollable: CodeExecutionPoll) {
        database.getCollection<CodeExecutionRequest>().updateOne(CodeExecutionPoll::id eq pollable.id, pollable)
    }

    fun saveCodeExecutionPoll(poll: CodeExecutionPoll) {
        database.getCollection<CodeExecutionPoll>().save(poll)
    }

    fun updateJoinedResource(joinedResource: JoinedResource) {
        database.getCollection<JoinedResource>().updateOne(JoinedResource::resourceId eq joinedResource.resourceId, joinedResource)
    }

    fun findAllJoinedResources(): List<JoinedResource> {
        return database.getCollection<JoinedResource>().find().toList()
    }
}