package astah.plugin

import com.change_vision.jude.api.inf.model.*

class ButtonMakeInstance: ButtonInherit(InstanceOf::getAllDefinitionsWithLevels, "Make Instance")

class ButtonInheritFromSuperclasses:
    ButtonInherit(AstahAccessor::getAllSuperClassesWithLevels,"Inherit from superclasses")

object InstanceOf {
    fun getInstances(clazz: IClass) =
        AstahAccessor.getDirectChildrenWithStereotypes(clazz, "instance-of")
    fun getDefinitions(instance: IClass) =
        AstahAccessor.getAssociationClassesWithStereotypes(instance, "instance-of")

    fun getAllDefinitionsWithLevels(instance: IClass): List<Pair<IClass, Int>> {
        val definitions = mutableListOf<Pair<IClass, Int>>()
        val directDefinitions = getDefinitions(instance)
        definitions.addAll(directDefinitions.map { Pair(it, -1) })
        directDefinitions.forEach { definitions.addAll(AstahAccessor.getAllSuperClassesWithLevels(it)) }
        return definitions
    }

    fun isSelectedElementsInClassOrBlockDiagram(diagram: IDiagram?): Array<IElement>? {
        val edition = AstahAccessor.edition
        if (diagram == null || (edition == "sysML" && diagram !is IBlockDefinitionDiagram) ||
            (edition == "professional" && diagram !is IClassDiagram)) {
            println("the current diagram is not supported")
            return null
        }

        val selectedElements = AstahAccessor.getSelectedElements()
        if (selectedElements == null) {
            println("Select any element")
        }

        return selectedElements
    }
}