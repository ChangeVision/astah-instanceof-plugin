package astah.plugin

import com.change_vision.jude.api.inf.model.IAttribute
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IDiagram
import javax.swing.JButton

open class ButtonInherit(val getTargetClasses: (IClass) -> List<Pair<IClass, Int>>, label: String): JButton(label) {
    fun push(diagram: IDiagram?) {
        val selectedElements = InstanceOf.isSelectedElementsInClassOrBlockDiagram(diagram) ?: return
        val selectedClasses = selectedElements.filterIsInstance<IClass>()
        if (selectedClasses.isEmpty()) {
            println("Select any class or block")
            return
        }
        selectedClasses.forEach { clazz ->
            val targetClasses = getTargetClasses(clazz)
            val attributes = mutableSetOf<Pair<IAttribute, Int>>()
            val inheritedAttributes = mutableSetOf<Pair<IAttribute, Int>>()
            val inheritedStereotypes = mutableSetOf<String>()
            targetClasses.forEach { superclass ->
                attributes += AstahAccessor.getAttributes(superclass.first).map { Pair(it, superclass.second) }
                inheritedStereotypes += superclass.first.stereotypes
            }

            attributes.forEach { attribute ->
                if (!inheritedAttributes.map { it.first.name }.contains(attribute.first.name) ||
                    inheritedAttributes.filter { it.first.name == attribute.first.name }
                        .all { it.second > attribute.second }) {
                    inheritedAttributes += attribute
                }
            }

            inheritedAttributes.forEach { attribute ->
                val name = attribute.first.name
                val type = attribute.first.type
                val initialValue = attribute.first.initialValue
                if (!AstahAccessor.getAttributes(clazz).map { it.name }.contains(name))
                    AstahAccessor.createAttribute(clazz, name, type, initialValue)
            }
            inheritedStereotypes.forEach { AstahAccessor.addStereotype(clazz, it) }
        }
    }
}