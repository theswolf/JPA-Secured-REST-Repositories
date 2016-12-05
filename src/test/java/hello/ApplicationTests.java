/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder input) {
		return input.with(user("user")).with(csrf());
	}

	private MockHttpServletRequestBuilder auth2(MockHttpServletRequestBuilder input) {
		return input.with(
				user(new User("user2","password2",Arrays.asList(new SimpleGrantedAuthority("USER"))))
		).with(csrf());
	}



	@Autowired
	private PersonRepository personRepository;

	@Before
	public void deleteAllBeforeTests() throws Exception {
		mockMvc.perform(auth(delete("/people")));
		mockMvc.perform(auth2(delete("/people")));


	}

	@Test
	@WithMockUser
	public void shouldReturnRepositoryIndex() throws Exception {

		mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk()).andExpect(
				jsonPath("$._links.people").exists());
	}

	@Test
	@WithMockUser
	public void shouldCreateEntity() throws Exception {

		mockMvc.perform(auth(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated()).andExpect(
								header().string("Location", containsString("people/")));
	}

	@Test
	public void shouldRetrieveEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");
		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Frodo")).andExpect(
						jsonPath("$.lastName").value("Baggins"));
	}

	@Test
	public void shouldQueryEntity() throws Exception {

		mockMvc.perform(auth(post("/people")).content(
				"{ \"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated());

		mockMvc.perform(
				auth(get("/people/search/findByLastName?name={name}", "Baggins"))).andExpect(
						status().isOk()).andExpect(
								jsonPath("$._embedded.people[0].firstName").value(
										"Frodo"));
	}

	@Test
	public void shouldUpdateEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(auth(put(location)).content(
				"{\"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isNoContent());

		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Bilbo")).andExpect(
						jsonPath("$.lastName").value("Baggins"));
	}

	@Test
	public void shouldNotUpdateEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth2(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
				status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(auth(put(location)).content(
				"{\"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
				status().isForbidden());

		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Frodo")).andExpect(
				jsonPath("$.lastName").value("Baggins"));
	}

	@Test
	public void shouldPartiallyUpdateEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(
				auth(patch(location)).content("{\"firstName\": \"Bilbo Jr.\"}")).andExpect(
						status().isNoContent());

		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Bilbo Jr.")).andExpect(
						jsonPath("$.lastName").value("Baggins"));
	}

	@Test
	public void shouldDeleteEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth(post("/people")).content(
				"{ \"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");
		mockMvc.perform(auth(delete(location))).andExpect(status().isNoContent());

		mockMvc.perform(auth(get(location))).andExpect(status().isNotFound());
	}

	@Test
	public void shouldNotDeleteEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth2(post("/people")).content(
				"{ \"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
				status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(auth(delete(location))).andExpect(status().isForbidden());

		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Bilbo")).andExpect(
				jsonPath("$.lastName").value("Baggins"));

	}
}