package hello;

import hello.data.AbsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.*;

@Entity
public class Person extends AbsModel<PersonRepository>{


	private String firstName;
	private String lastName;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}


}
