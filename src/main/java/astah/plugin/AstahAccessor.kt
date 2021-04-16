package astah.plugin

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.editor.TransactionManager
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.IPresentation
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.project.ProjectEventListener
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener
import com.change_vision.jude.api.inf.view.IEntitySelectionListener

object AstahAccessor {
    private val projectAccessor: ProjectAccessor = AstahAPI.getAstahAPI().projectAccessor
    val edition: String = projectAccessor.astahEdition
    val defaultType: String = if (edition == "sysML") "String" else "Object"

    fun getCurrentDiagram(): IDiagram? = projectAccessor.viewManager.diagramViewManager.currentDiagram

    private fun isEndOwnerIClassWithRole(sourceNavigability: String,
                                         targetNavigability: String,
                                         sourceBlock: IClass, association: IAssociation):
            Pair<IClass, String>? {
        val memberEnds = association.memberEnds
        val direction = memberEnds[0].owner != sourceBlock
        val associationEndOwner = if (direction) memberEnds[0].owner else memberEnds[1].owner
        val isUnspecifiedToNavigable =
            if (direction)
                memberEnds[0].navigability == sourceNavigability && memberEnds[1].navigability == targetNavigability
            else
                memberEnds[0].navigability == targetNavigability && memberEnds[1].navigability == sourceNavigability
        val role = if (direction) memberEnds[1].name else memberEnds[0].name
        return if (isUnspecifiedToNavigable && associationEndOwner is IClass) Pair(associationEndOwner, role) else null
    }

    private fun isUnspecifiedToNavigableEndOwnerIClassWithRole(block: IClass, association: IAssociation):
            Pair<IClass, String>? =
         isEndOwnerIClassWithRole("Unspecified", "Navigable", block, association)

    private fun isNavigableToUnspecifiedEndOwnerIClassWithRole(block: IClass, association: IAssociation):
            Pair<IClass, String>? =
        isEndOwnerIClassWithRole("Navigable","Unspecified" , block, association)

    private fun isUnspecifiedToNavigableEndOwnerIClass(block: IClass, association: IAssociation): IClass? =
        isUnspecifiedToNavigableEndOwnerIClassWithRole(block, association)?.first

    private fun isNavigableToUnspecifiedEndOwnerIClass(block: IClass, association: IAssociation): IClass? =
        isNavigableToUnspecifiedEndOwnerIClassWithRole(block, association)?.first

    fun getAllClasses(): List<IClass> = getAllClasses(projectAccessor.project)

    fun getAllClasses(element: INamedElement): List<IClass> {
        val ret = mutableListOf<IClass>()
        if (element is IPackage) {
            element.ownedElements.filterIsInstance<IClass>().forEach { ownedClass ->
                ret += ownedClass
                ret.addAll(getAllSuperClasses(ownedClass))
            }
            element.ownedElements.filterIsInstance<IPackage>().forEach { ret.addAll(getAllClasses(it)) }
        }
        return ret
    }

    fun getAssociationClassesWithStereotypes(block: IClass, stereotype: String): List<IClass> {
        val associatedClasses = mutableListOf<IClass>()
        block.attributes.forEach { attribute ->
            attribute.association?.let { association ->
                if (association.hasStereotype(stereotype)) {
                    isUnspecifiedToNavigableEndOwnerIClass(block, association)?.let { endOwner ->
                        associatedClasses += endOwner
                    }
                }
            }
        }
        return associatedClasses
    }

    fun getDirectChildrenWithStereotypes(block: IClass, stereotype: String): List<IClass> {
        val associatedClasses = mutableListOf<IClass>()
        block.attributes.forEach { attribute ->
            attribute.association?.let { association ->
                if (association.hasStereotype(stereotype)) {
                    isNavigableToUnspecifiedEndOwnerIClass(block, association)?.let { endOwner ->
                        associatedClasses += endOwner
                    }
                }
            }
        }
        return associatedClasses
    }

    fun addStereotype(association: IAssociation, stereotype: String) {
        if (!association.hasStereotype(stereotype)) {
            try {
                TransactionManager.beginTransaction()
                association.addStereotype(stereotype)
                TransactionManager.endTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
                if (TransactionManager.isInTransaction()) {
                    TransactionManager.abortTransaction()
                }
            }
        }
    }

    fun addStereotype(clazz: IClass, stereotype: String) {
        if (!clazz.hasStereotype(stereotype)) {
            try {
                TransactionManager.beginTransaction()
                clazz.addStereotype(stereotype)
                TransactionManager.endTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
                if (TransactionManager.isInTransaction()) {
                    TransactionManager.abortTransaction()
                }
            }
        }
    }
    fun getAllSuperClasses(block: IClass): List<IClass> =
        getAllSuperClassesWithLevels(block).map { it.first }


    fun getAllSuperClassesWithLevels(block: IClass): List<Pair<IClass, Int>> {
        fun getSuperClasses2(block2: IClass, level: Int): List<Pair<IClass, Int>> {
            fun getSuperClasses1(block1: IClass): List<IClass> {
                val superClasses = mutableListOf<IClass>()
                block1.generalizations.forEach { generalization ->
                    generalization.superType?.let { superType ->
                        superClasses += superType
                    }
                }
                return superClasses
            }
            val directSuperClasses = getSuperClasses1(block2)
            val allSuperClasses = mutableListOf<Pair<IClass, Int>>()
            directSuperClasses.forEach { directSuperClass ->
                allSuperClasses += Pair(directSuperClass, level)
                allSuperClasses += getSuperClasses2(directSuperClass, level + 1)
            }
            return allSuperClasses
        }
        return getSuperClasses2(block, 0)
    }

    fun getDirectSubClasses(block: IClass): List<IClass> {
        val subClasses = mutableListOf<IClass>()
        block.specializations.forEach { specialization ->
            specialization.subType?.let { subType ->
                subClasses += subType
            }
        }
        return subClasses
    }

    fun getAllSubclasses(clazz: IClass): List<IClass> {
        val allSubclasses = mutableListOf<IClass>()
        val directSubclasses = getDirectSubClasses(clazz)
        allSubclasses += directSubclasses
        directSubclasses.forEach { directSubclass ->
            allSubclasses += getAllSubclasses(directSubclass)
        }
        return allSubclasses
    }

    fun getAttributes(block: IClass): List<IAttribute> = block.attributes.filter { it.association == null }

    private fun createAttribute(clazz: IClass, name: String, type: IClass?, initValue: String?,
                                createAttributeWithoutType: (IClass?, String?, String?) -> IAttribute,
                                createAttributeWithType: (IClass?, String?, IClass?) -> IAttribute?): IAttribute? {
        var attribute: IAttribute? = null
        try {
            TransactionManager.beginTransaction()
            attribute =
                if (type == null)
                    createAttributeWithoutType(clazz, name, defaultType)
                else
                    createAttributeWithType(clazz, name, type)
            initValue?.let { init -> attribute?.let { it.initialValue = init } }
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
        return attribute
    }

    fun setInitialValue(attribute: IAttribute, value: String) {
        try {
            TransactionManager.beginTransaction()
            attribute.initialValue = value
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setType(attribute: IAttribute, type: IClass) {
        try {
            TransactionManager.beginTransaction()
            attribute.type = type
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun createAttributeOfClass(clazz: IClass, name: String, type: IClass?, initValue: String?): IAttribute? {
        if (edition == "sysML") error("")
        val modelEditor =projectAccessor.modelEditorFactory.basicModelEditor
        return createAttribute(clazz, name, type, initValue,
            fun (c: IClass?, n: String?, t: String?) = modelEditor.createAttribute(c,n,t),
            fun (c: IClass?, n: String?, t: IClass?) = modelEditor.createAttribute(c,n,t))
    }

    fun createValueAttribute(clazz: IClass, name: String, type: IClass?, initValue: String?): IAttribute? {
        if (edition != "sysML") error("")
        val modelEditor = projectAccessor.modelEditorFactory.sysmlModelEditor
        return createAttribute(clazz, name, type, initValue,
            fun (c: IClass?, n: String?, t: String?) = modelEditor.createValueAttribute(c, n, t),
            fun (c: IClass?, n: String?, t: IClass?) = modelEditor.createValueAttribute(c, n, t))
    }

    fun createAttribute(clazz: IClass, name: String, type: IClass?, initValue: String?): IAttribute? =
        if (edition == "sysML")
            createValueAttribute(clazz, name, type, initValue)
        else
            createAttributeOfClass(clazz, name, type, initValue)

    fun getSelectedElements(): Array<IElement>? =
        projectAccessor.viewManager.diagramViewManager.selectedElements
    fun getSelectedPresentations(): Array<IPresentation>? =
        projectAccessor.viewManager.diagramViewManager.selectedPresentations

    fun addProjectEventListener(listener: ProjectEventListener) {
        try {
            projectAccessor.addProjectEventListener(listener)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    fun addIDiagramEditorSelectionListener(listener: IDiagramEditorSelectionListener) {
        try {
            projectAccessor.viewManager.diagramViewManager.addDiagramEditorSelectionListener(listener)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    fun addEntitySelectionListener(listener: IEntitySelectionListener) {
        try {
            projectAccessor.viewManager.diagramViewManager.addEntitySelectionListener(listener)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }
}