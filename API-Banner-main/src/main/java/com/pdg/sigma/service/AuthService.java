package com.pdg.sigma.service;

import com.pdg.sigma.domain.DepartmentHead;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.Prospect;
import com.pdg.sigma.dto.AuthDTO;
import com.pdg.sigma.repository.DepartmentHeadRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.ProspectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;

    public String loginUser(AuthDTO auth) throws Exception{
        Optional<Prospect> student = prospectRepository.findById(auth.getUserId());
        Optional<Professor> professor = professorRepository.findById(auth.getUserId());
        Optional<DepartmentHead> departmentHead = departmentHeadRepository.findById(auth.getUserId());

        if(departmentHead.isPresent()){
            if(departmentHead.get().getPassword().equals(auth.getPassword())){
                return "jfedpto";
            }
            else
                return "false";
        }
        if(student.isPresent()){
            if(student.get().getPassword().equals(auth.getPassword())){
                return "student";
            }
            else
                return "false";
        }
        else if(professor.isPresent()) {

            if(professor.get().getPassword().equals(auth.getPassword())){
                return "professor";
            }
            else
                return "false";

        }
        else
            return "false all";
    }
}
