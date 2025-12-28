package BlueMoon.bluemoon.daos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.PhanHoi;
import jakarta.persistence.EntityManager;

@Repository
public class PhanHoiDAO {
    @Autowired private EntityManager entityManager;
    public void save(PhanHoi ph) {
        if (ph == null){
            entityManager.persist(ph);
        } else {
            entityManager.merge(ph);
        }
    }
    
}
