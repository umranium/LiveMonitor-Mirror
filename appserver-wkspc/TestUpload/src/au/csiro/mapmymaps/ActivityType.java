/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.csiro.mapmymaps;

/**
 *
 * @author abd01c
 */
public enum ActivityType {
	RUNNING("running"),
	CYCLING("cycling"),
	MOUNTAIN_BIKING("mountain biking"),
	SAILING("sailing"),
	DRIVING("driving"),
	WALKING("walking"),
	SWIMMING("swimming"),
	SKIING("skiing"),
	MOTOR_RACING("motor racing"),
	MOTORCYCLING("motorcycling"),
	ENDURO("enduro"),
	CANOEING("canoeing"),
	KAYAKING("kayaking"),
	SEA_KAYAKING("sea kayaking"),
	ROWING("rowing"),
	WINDSURFING("windsurfing"),
	KITEBOARDING("kiteboarding"),
	ORIENTEERING("orienteering"),
	MOUNTAINEERING("mountaineering"),
	SKATING("skating"),
	SKATEBOARDING("skateboarding"),
	HORSE_RIDING("horse riding"),
	HANG_GLIDING("hang gliding"),
	GLIDING("gliding"),
	SNOWBOARDING("snowboarding"),
	PARAGLIDING("paragliding"),
	HOT_AIR_BALLOONING("hot air ballooning"),
	NORDIC_WALKING("nordic walking"),
	MISCELLANEOUS("miscellaneous");
	
	private String name;

	private ActivityType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
	
}
