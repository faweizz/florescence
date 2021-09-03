package de.faweizz.topicservice.service.sharing

class TenantIsOwnerOfTopicException(ownerTenantName: String, name: String) :
    Throwable("$ownerTenantName is owner of topic $name")
